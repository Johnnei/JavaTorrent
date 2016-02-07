package org.johnnei.javatorrent.internal.tracker.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.torrent.tracker.TrackerManager;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around the UDP logic for testability and separation of IO interaction.
 *
 */
public class UdpTrackerSocket implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UdpTrackerSocket.class);

	private final Clock clock;

	private final TrackerManager trackerManager;

	private volatile boolean keepRunning = true;

	/**
	 * The lock protecting the state of the tracker request map {@link #pendingRespones} and
	 * {@link #unsentRequests}
	 */
	private final Object lock = new Object();

	/**
	 * Map containing all tracker requests which didn't have a response yet.
	 * Mapped: TransactionID to request
	 */
	private Map<Integer, SentRequest> pendingRespones;

	/**
	 * Requests which are waiting for a connection id to arrive
	 */
	private Collection<UnsetRequest> unsentRequests;

	private UdpSocketUtils socketUtils;

	private DatagramSocket udpSocket;

	/**
	 * A lock providing condition objects to signal new tasks
	 */
	private final Lock taskLock = new ReentrantLock();

	/**
	 * Condition
	 */
	private final Condition newWork;

	private UdpTrackerSocket(Builder builder) throws TrackerException {
		clock = builder.clock;
		newWork = taskLock.newCondition();
		trackerManager = builder.trackerManager;
		socketUtils = builder.socketUtils;
		try {
			udpSocket = new DatagramSocket(builder.socketPort);
			udpSocket.setSoTimeout((int) Duration.of(5, ChronoUnit.SECONDS).toMillis());
		} catch (SocketException e) {
			throw new TrackerException("Failed to create UDP Socket", e);
		}
		pendingRespones = new HashMap<>();
		unsentRequests = new LinkedList<>();
	}

	/**
	 * Submits the request to be send to the tracker.
	 *
	 * @param tracker The tracker which is sending this request
	 * @param request The request to send
	 */
	public void submitRequest(UdpTracker tracker, IUdpTrackerPayload request) {
		// Queue the request for sending
		synchronized (lock) {
			unsentRequests.add(new UnsetRequest(tracker, request));
		}

		taskLock.lock();
		try {
			newWork.signalAll();
		} finally {
			taskLock.unlock();
		}
	}

	/**
	 * Runs the UDP Tracker worker thread until {@link #shutdown()} is invoked
	 */
	@Override
	public void run() {
		try {
			while (keepRunning) {
				processWork();
				waitForWork();
			}
		} finally {
			// Always clean up the socket
			udpSocket.close();
		}
	}

	private void waitForWork() {
		if (!pendingRespones.isEmpty()) {
			return;
		}

		if (!unsentRequests.isEmpty()) {
			return;
		}

		if (!keepRunning) {
			return;
		}

		taskLock.lock();
		try {
			newWork.awaitUninterruptibly();
		} finally {
			taskLock.unlock();
		}
	}

	private void processWork() {
		if (!unsentRequests.isEmpty()) {
			sendRequests();
		}

		Collection<SentRequest> timedoutRequests = findTimedoutRequests();
		if (!timedoutRequests.isEmpty()) {
			resendRequests(timedoutRequests);
		}

		if (!pendingRespones.isEmpty()) {
			receiveResponse();
		}
	}

	private void sendRequests() {
		Collection<UnsetRequest> unsentRequestsCopy;
		synchronized (lock) {
			unsentRequestsCopy = new ArrayList<>(unsentRequests);
		}
		for (UnsetRequest request : unsentRequestsCopy) {
			if (!canSendRequest(request.tracker, request.payload.getAction())) {
				continue;
			}

			TrackerRequest wrappedRequest = new TrackerRequest(request.tracker, trackerManager.createUniqueTransactionId(), request.payload);

			// List this packet as sent before the actual write to prevent race conditions
			synchronized (lock) {
				pendingRespones.put(wrappedRequest.getTransactionId(), new SentRequest(wrappedRequest, clock));
			}

			try {
				writeRequest(wrappedRequest);
				// When the write call completed successfully we will remove the request from the queue
				synchronized (lock) {
					unsentRequests.remove(request);
				}
			} catch (IOException e) {
				LOGGER.warn("Tracker request failed to write: {}, resubmitted request.", wrappedRequest, e);
				synchronized (lock) {
					pendingRespones.remove(wrappedRequest.getTransactionId());
				}
			}
		}
	}

	private Collection<SentRequest> findTimedoutRequests() {
		synchronized (lock) {
			return pendingRespones.values().stream()
					.filter(SentRequest::isTimedout)
					.collect(Collectors.toList());
		}
	}

	private void resendRequests(Collection<SentRequest> timedoutRequests) {
		for (SentRequest pendingResponse : timedoutRequests) {
			if (!canResendRequest(pendingResponse)) {
				continue;
			}

			try {
				writeRequest(pendingResponse.request);
				pendingResponse.attempt++;
			} catch (IOException e) {
				LOGGER.warn("Tracker request failed to write: {}. Delayed resend of timedout request.", pendingResponse.request, e);
			}
		}
	}

	private boolean canResendRequest(SentRequest pendingResponse) {
		if (pendingResponse.attempt == 8) {
			LOGGER.warn("Tracker failed to respond to {} after 8 attempts. Discarding request.", pendingResponse.request);
			synchronized (lock) {
				pendingRespones.remove(pendingResponse.request.getTransactionId());
			}
			pendingResponse.request.onFailure();
			return false;
		}

		if (!canSendRequest(pendingResponse.request.getTracker(), pendingResponse.request.getAction())) {
			return false;
		}

		return true;
	}

	private boolean canSendRequest(UdpTracker tracker, TrackerAction action) {
		if (tracker.getConnection().isValidFor(action, clock)) {
			return true;
		}

		// Need to obtain a new connection token first.
		if (!isConnectRequestQueued(tracker)) {
			// No connect request has been sent or current one is expired.
			// Reset it to a new connect-state connection and submit connection request.
			LOGGER.debug("Refreshing connection ID for tracker: {}.", tracker);
			tracker.setConnection(new Connection(clock));
			submitRequest(tracker, new ConnectionRequest(clock));
		}

		// Don't sent this request just yet as the connection isn't valid
		return false;
	}

	private void writeRequest(TrackerRequest wrappedRequest) throws IOException {
		LOGGER.trace("Sending tracker request: {}.", wrappedRequest);
		OutStream outStream = new OutStream();
		wrappedRequest.writeRequest(outStream);
		socketUtils.write(udpSocket, wrappedRequest.getTracker().getSocketAddress(), outStream);
	}

	/**
	 * Checks if one of the queues contains a {@link TrackerAction#CONNECT} request.
	 *
	 * @param tracker
	 * @return
	 */
	private boolean isConnectRequestQueued(UdpTracker tracker) {
		synchronized (lock) {
			if (pendingRespones.values().stream()
					.filter(pendingRequest -> pendingRequest.request.getTracker().equals(tracker))
					.anyMatch(pendingRequest -> pendingRequest.request.getAction() == TrackerAction.CONNECT)) {
				return true;
			}

			if (unsentRequests.stream()
					.filter(unsentRequest -> unsentRequest.tracker.equals(tracker))
					.anyMatch(unsetRequest -> unsetRequest.payload.getAction() == TrackerAction.CONNECT)) {
				return true;
			}
		}

		return false;
	}

	private void receiveResponse() {
		SentRequest sentRequest;
		InStream inStream = null;
		try {
			// Read packet and find the corresponding request
			inStream = socketUtils.read(udpSocket);
		} catch (IOException e) {
			LOGGER.warn("Tracker request failed to read.", e);
			return;
		}

		int transactionId = peekTransactionId(inStream);

		synchronized (lock) {
			sentRequest = pendingRespones.remove(transactionId);
		}

		if (sentRequest == null) {
			LOGGER.warn("Received response with an unknown transaction id: {}", transactionId);
			return;
		}
		try {
			sentRequest.request.readResponse(inStream);
			sentRequest.request.process();
		} catch (TrackerException e) {
			LOGGER.warn("Failed to process tracker response for {}", sentRequest.request, e);
		}
	}

	public void shutdown() {
		keepRunning = false;
		taskLock.lock();
		try {
			newWork.signalAll();
		} finally {
			taskLock.unlock();
		}
	}

	private int peekTransactionId(InStream inStream) {
		inStream.mark();
		// Ignore the 'action' field
		inStream.readInt();

		int transactionId = inStream.readInt();
		inStream.resetToMark();
		return transactionId;
	}

	private static final class UnsetRequest {

		private final IUdpTrackerPayload payload;

		private final UdpTracker tracker;

		public UnsetRequest(UdpTracker tracker, IUdpTrackerPayload payload) {
			this.tracker = tracker;
			this.payload = payload;
		}

	}

	private static final class SentRequest {

		private final TrackerRequest request;

		private final Clock clock;

		private LocalDateTime sentTime;

		private int attempt;

		public SentRequest(TrackerRequest request, Clock clock) {
			this.request = request;
			this.clock = clock;
			sentTime = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
		}

		public boolean isTimedout() {
			Duration timeoutPeriod = Duration.ofSeconds(15L * (int) Math.pow(2, attempt));
			Duration timeSinceRequest = Duration.between(sentTime, LocalDateTime.now(clock));
			if (timeSinceRequest.minus(timeoutPeriod).isNegative()) {
				LOGGER.trace("Request not timed out: {}, Sent: {}", LocalDateTime.now(clock), sentTime);
				// There is still some time left before the timeout hits
				return false;
			}

			return true;
		}
	}

	public static final class Builder {

		private TrackerManager trackerManager;

		private UdpSocketUtils socketUtils;

		private int socketPort;

		private Clock clock;

		public Builder() {
			clock = Clock.systemDefaultZone();
		}

		public Builder setClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		public Builder setTrackerManager(TrackerManager trackerManager) {
			this.trackerManager = trackerManager;
			return this;
		}

		public Builder setSocketUtils(UdpSocketUtils socketUtils) {
			this.socketUtils = socketUtils;
			return this;
		}

		public Builder setSocketPort(int socketPort) {
			this.socketPort = socketPort;
			return this;
		}

		public UdpTrackerSocket build() throws TrackerException {
			return new UdpTrackerSocket(this);
		}
	}

}
