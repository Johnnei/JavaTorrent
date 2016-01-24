package org.johnnei.javatorrent.network.protocol.utp;

public enum ConnectionState {
	
	/**
	 * Connecting to other side
	 */
	CONNECTING, 
	/**
	 * Connected to other side
	 */
	CONNECTED, 
	/**
	 * Closing connection in the official manner
	 */
	DISCONNECTING,
	/**
	 * Connection got closed (Either normally or by a reset)
	 */
	CLOSED

}
