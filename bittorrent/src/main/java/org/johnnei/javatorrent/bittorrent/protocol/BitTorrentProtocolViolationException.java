package org.johnnei.javatorrent.bittorrent.protocol;

/**
 * Exception to throw when the remote end violates the contract of the BitTorrent protocol.
 */
public class BitTorrentProtocolViolationException extends RuntimeException {

	/**
	 * Creates a new Protocol Violation exception.
	 * @param message The reason why the protocol was violated.
	 */
	public BitTorrentProtocolViolationException(String message) {
		super(message);
	}

	/**
	 * Creates a new Protocol Violation exception.
	 * @param message The reason why the protocol was violated.
	 * @param cause The underlying cause of the violation.
	 */
	public BitTorrentProtocolViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}
