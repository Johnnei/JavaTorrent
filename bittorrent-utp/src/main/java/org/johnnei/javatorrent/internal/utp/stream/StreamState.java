package org.johnnei.javatorrent.internal.utp.stream;

/**
 * Represents the state of an InputStream.
 */
public enum StreamState {

	/**
	 * Stream is still sending data.
	 */
	ACTIVE,
	/**
	 * Stream is waiting until all data is send in order to sen the FIN packet.
	 */
	SHUTDOWN_PENDING,
	/**
	 * FIN has been sent/received.
	 */
	SHUTDOWN
}
