package org.johnnei.javatorrent.internal.tracker.udp;

import java.util.Objects;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.UdpTracker;

/**
 * Represents a request to a tracker.
 *
 */
public class TrackerRequest {

	/**
	 * The tracker who submitted this request
	 */
	private UdpTracker tracker;

	/**
	 * The payload of this tracker request
	 */
	private final IUdpTrackerPayload message;

	/**
	 * The ID of this transaction
	 */
	private final int transactionId;

	/**
	 * Creates a new request for an {@link UdpTracker}
	 * @param tracker The tracker for whom this request will be executed
	 * @param transactionId The assigned transaction id
	 * @param message The payload of the actual request.
	 */
	public TrackerRequest(final UdpTracker tracker, final int transactionId, final IUdpTrackerPayload message) {
		this.tracker = Objects.requireNonNull(tracker);
		this.transactionId = transactionId;
		this.message = Objects.requireNonNull(message);
	}

	/**
	 * Writes the request
	 * @param outStream The stream to write on
	 */
	public void writeRequest(OutStream outStream) {
		outStream.writeLong(tracker.getConnection().getId());
		outStream.writeInt(message.getAction().getId());
		outStream.writeInt(transactionId);
		message.writeRequest(outStream);
	}

	/**
	 * Reads the request response
	 * @param inStream
	 * @throws TrackerException
	 */
	public void readResponse(InStream inStream) throws TrackerException {
		TrackerAction action = TrackerAction.of(inStream.readInt());
		if (action == TrackerAction.ERROR) {
			String error = inStream.readString(inStream.available());
			throw new TrackerException(String.format("Tracker responded with an error: %s", error));
		}

		int responseTransactionId = inStream.readInt();
		if (responseTransactionId != transactionId) {
			throw new TrackerException(String.format(
					"Expected transaction id %d but found %d, rejecting response.",
					transactionId,
					responseTransactionId));
		}

		message.readResponse(inStream);
	}

	/**
	 * Processes the received response
	 */
	public void process() {
		message.process(tracker);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "TrackerRequest [transactionId=" + transactionId + ", message=" + message + "]";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + transactionId;
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TrackerRequest)) {
			return false;
		}
		TrackerRequest other = (TrackerRequest) obj;
		if (transactionId != other.transactionId) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the action type of this request
	 * @return The action
	 */
	public TrackerAction getAction() {
		return message.getAction();
	}

	/**
	 * Gets the assigned transaction id for this request
	 * @return The transaction id
	 */
	public int getTransactionId() {
		return transactionId;
	}

	/**
	 * Gets the tracker for which this request will be executed.
	 * @return The tracker
	 */
	public UdpTracker getTracker() {
		return tracker;
	}

	/**
	 * Called when the request must be reported to the tracker as failed.
	 */
	public void onFailure() {
		tracker.onRequestFailed(message);
	}

}
