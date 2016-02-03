package org.johnnei.javatorrent.download.tracker.udp;

import java.util.Objects;

import org.johnnei.javatorrent.download.tracker.UdpTracker;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerException;

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

	public TrackerRequest(final UdpTracker tracker, final int transactionId, final IUdpTrackerPayload message) {
		this.transactionId = transactionId;
		this.message = Objects.requireNonNull(message);
	}

	/**
	 * Writes the request
	 * @param outStream The stream to write on
	 * @param connection The connection to use
	 */
	public void writeRequest(OutStream outStream, Connection connection) {
		outStream.writeLong(connection.getId());
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
		int responseTransactionId = inStream.readInt();
		if (responseTransactionId != transactionId) {
			throw new TrackerException(String.format(
					"Expected transaction id %d but found %d, rejecting response.",
					transactionId,
					responseTransactionId));
		}

		if (action == TrackerAction.ERROR) {
			String error = inStream.readString(inStream.available());
			throw new TrackerException(String.format("Tracker responded with an error: %s", error));
		}

		message.readResponse(inStream);
	}

	public void process() {
		message.process(tracker);
	}

	@Override
	public String toString() {
		return "TrackerRequest [transactionId=" + transactionId + ", message=" + message + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + transactionId;
		return result;
	}

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

	public TrackerAction getAction() {
		return message.getAction();
	}

	public int getTransactionId() {
		return transactionId;
	}

	public UdpTracker getTracker() {
		return tracker;
	}

}
