package org.johnnei.javatorrent.tracker.udp;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import org.johnnei.javatorrent.torrent.tracker.TrackerAction;

public class Connection {

	private static final Duration LIFE_TIME = Duration.ofMinutes(1);

	private static final long NO_CONNECTION_ID = 0x41727101980L;

	/**
	 * The ID of the connection with the tracker
	 */
	private final long connnectionId;

	/**
	 * The time at which this ID was received.
	 */
	private final LocalDateTime creationTime;

	/**
	 * Creates a connection with the {@link #NO_CONNECTION_ID} as ID
	 * @param clock The current time instance
	 */
	public Connection(Clock clock) {
		this(NO_CONNECTION_ID, clock);
	}

	/**
	 * Creates a connection with the given connection id
	 * @param connectionId The connection id
	 * @param clock The current time instance
	 */
	public Connection(final long connectionId, Clock clock) {
		this.connnectionId = connectionId;
		creationTime = LocalDateTime.now(clock);
	}

	/**
	 * Verifies if this connection can be used to submit the request at the tracker.
	 * @param action The action for which the connection should be valid
	 * @param clock The current time instance
	 * @return true if this connection can be used for the request
	 */
	public boolean isValidFor(TrackerAction action, Clock clock) {
		if (action == TrackerAction.CONNECT) {
			return connnectionId == NO_CONNECTION_ID;
		}

		if (connnectionId == NO_CONNECTION_ID) {
			return false;
		}

		if (!LocalDateTime.now(clock).minus(LIFE_TIME).isBefore(creationTime)) {
			return false;
		}

		return true;
	}

	/**
	 * The ID of the connection with the tracker
	 * @return The connection ID
	 */
	public long getId() {
		return connnectionId;
	}

}
