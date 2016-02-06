package org.johnnei.javatorrent.tracker.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	private final Clock clock = Clock.systemDefaultZone();

	private final TrackerManager trackerManager;

	private volatile boolean keepRunning = true;

	/**
	 * The lock protecting the state of the tracker request map {@link #pendingRespones} and {@link #unsentRequests}
	 */
	private final Object lock = new Object();

	/**
	 * Map containing all tracker requests which didn't have a response yet.
	 * Mapped: TransactionID to request
	 */
	private Map<Integer, TrackerRequest> pendingRespones;

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

	@Override
	public void run() {
		while (keepRunning) {
			processWork();
			waitForWork();
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
			if (!request.tracker.getConnection().isValidFor(request.payload.getAction(), clock)) {
				// Need to obtain a new connection token first.
				if (!isConnectRequestQueued(request.tracker)) {
					// No connect request has been sent or current one is expired.
					// Reset it to a new connect-state connection and submit connection request.
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("Refreshing connection ID for tracker: %s.", request.tracker));
					}
					request.tracker.setConnection(new Connection(clock));
					submitRequest(request.tracker, new ConnectionRequest(clock));
				}

				// Don't sent this request just yet as the connection isn't valid
				continue;
			}

			TrackerRequest wrappedRequest = new TrackerRequest(request.tracker, trackerManager.createUniqueTransactionId(), request.payload);
			// Send request
			OutStream outStream = new OutStream();
			wrappedRequest.writeRequest(outStream);

			// List this packet as sent before the actual write to prevent race conditions
			synchronized (lock) {
				pendingRespones.put(wrappedRequest.getTransactionId(), wrappedRequest);
			}

			// Send package
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("Sending tracker request: %s", wrappedRequest));
			}
			try {
				socketUtils.write(udpSocket, request.tracker.getSocketAddress(), outStream);
				// When the write call completed successfully we will remove the request from the queue
				synchronized (lock) {
					unsentRequests.remove(request);
				}
			} catch (IOException e) {
				LOGGER.warn(String.format("Tracker request failed to write: %s, resubmitted request.", wrappedRequest), e);
				synchronized (lock) {
					pendingRespones.remove(wrappedRequest.getTransactionId());
				}
			}
		}
	}

	/**
	 * Checks if one of the queues contains a {@link TrackerAction#CONNECT} request.
	 * @param tracker
	 * @return
	 */
	private boolean isConnectRequestQueued(UdpTracker tracker) {
		synchronized (lock) {
			if (pendingRespones.values().stream()
					.filter(pendingRequest -> pendingRequest.getTracker().equals(tracker))
					.anyMatch(pendingRequest -> pendingRequest.getAction() == TrackerAction.CONNECT)) {
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
		TrackerRequest response = null;
		try {
			// Read packet and find the corresponding request
			InStream inStream = socketUtils.read(udpSocket);
			int transactionId = peekTransactionId(inStream);

			synchronized (lock) {
				response = pendingRespones.remove(transactionId);
			}
			if (response == null) {
				LOGGER.warn(String.format("Received response with an unknown transaction id: %d", transactionId));
				return;
			}

			response.readResponse(inStream);
			response.process();
		} catch (IOException e) {
			LOGGER.warn(String.format("Tracker request failed to read.", response), e);
		} catch (TrackerException e) {
			LOGGER.warn(String.format("Failed to process tracker response for %s", response), e);
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

	public static final class Builder {

		private TrackerManager trackerManager;

		private UdpSocketUtils socketUtils;

		private int socketPort;

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
