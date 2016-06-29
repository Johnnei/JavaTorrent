package org.johnnei.javatorrent.internal.utp.protocol;

/**
 * The possible connection states of {@link org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl}
 */
public enum ConnectionState {

	/**
	 * Connecting to other side
	 */
	CONNECTING(false),
	/**
	 * Connected to other side
	 */
	CONNECTED(false),
	/**
	 * Closing connection in the official manner
	 */
	DISCONNECTING(true),
	/**
	 * Connection got closed (Either normally or by a reset)
	 */
	CLOSED(true);

	private final boolean isClosed;

	ConnectionState(boolean isClosed) {
		this.isClosed = isClosed;
	}

	/**
	 * Returns whether this state is considered closed for data.
	 * @return <code>true</code> when the state disallows data to be sent, otherwise <code>false</code>.
	 */
	public final boolean isClosedState() {
		return isClosed;
	}

}
