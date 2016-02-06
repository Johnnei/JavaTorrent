package org.johnnei.javatorrent.tracker.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

	private final Clock clock;

	private final TrackerManager trackerManager;

	private boolean keepRunning = true;

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
	private Queue<UnsetRequest> unsentRequests;

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

	public UdpTrackerSocket(TrackerManager trackerManager, int listeningPort, Clock clock) throws TrackerException {
		this.newWork = taskLock.newCondition();
		this.trackerManager = trackerManager;
		this.clock = clock;
		try {
			udpSocket = new DatagramSocket(listeningPort);
			udpSocket.setSoTimeout((int) Duration.of(5, ChronoUnit.SECONDS).toMillis());
		} catch (SocketException e) {
			throw new TrackerException("Failed to create UDP Socket", e);
		}
		pendingRespones = new HashMap<>();
		unsentRequests = new ConcurrentLinkedQueue<>();
		socketUtils = new UdpSocketUtils();
	}

	/**
	 * Submits the request to be send to the tracker.
	 * @param tracker The tracker which is sending this request
	 * @param request The request to send
	 */
	public void submitRequest(UdpTracker tracker, IUdpTrackerPayload request) {
		if (!tracker.getConnection().isValidFor(request.getAction(), clock)) {
			// Need to obtain a new connection token first.
			synchronized (lock) {
				LOGGER.debug(String.format("Postponing request of %s, refresh of Connection ID required..", request));
				if (pendingRespones.values().stream()
						.filter(pendingRequest -> pendingRequest.getTracker().equals(tracker))
						.noneMatch(pendingRequest -> pendingRequest.getAction() == TrackerAction.CONNECT)) {
					// No connect request has been sent yet, submit one.
					tracker.setConnection(new Connection(clock));
					submitRequest(tracker, new ConnectionRequest(clock));
				}

				// Queue the request until we receive a new connection id
				unsentRequests.add(new UnsetRequest(tracker, request));
				return;
			}
		}

		final TrackerRequest wrappedRequest = new TrackerRequest(tracker, trackerManager.createUniqueTransactionId(), request);
		synchronized (lock) {
			pendingRespones.put(wrappedRequest.getTransactionId(), wrappedRequest);
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
		for (UnsetRequest request : unsentRequests) {
			TrackerRequest wrappedRequest = new TrackerRequest(request.tracker, trackerManager.createUniqueTransactionId(), request.payload);
			try {
				// Send request
				OutStream outStream = new OutStream();
				wrappedRequest.writeRequest(outStream);

				// List this packet as sent before the actual write to prevent race conditions
				synchronized (lock) {
					pendingRespones.put(wrappedRequest.getTransactionId(), wrappedRequest);
				}

				// Send package
				socketUtils.write(udpSocket, request.tracker.getSocketAddress(), outStream);
			} catch (IOException e) {
				LOGGER.warn(String.format("Tracker request failed to write: %s, resubmitting request.", wrappedRequest), e);
				synchronized (lock) {
					pendingRespones.remove(wrappedRequest.getTransactionId());
				}
				submitRequest(request.tracker, request.payload);
			}
		}
	}

	private void receiveResponse() {
		TrackerRequest response = null;
		try {
			// Read packet and find the corresponding request
			InStream inStream = socketUtils.read(udpSocket);
			int transactionId = peekTransactionId(inStream);

			synchronized (lock) {
				response = pendingRespones.remove(transactionId);
				if (response == null) {
					LOGGER.warn(String.format("Received response with an unknown transaction id: %d", transactionId));
					return;
				}

				response.readResponse(inStream);
				response.process();
			}
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

}
