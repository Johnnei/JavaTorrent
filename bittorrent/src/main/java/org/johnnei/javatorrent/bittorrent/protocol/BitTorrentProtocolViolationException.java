package org.johnnei.javatorrent.bittorrent.protocol;

/**
 * Exception to throw when the remote end violates the contract of the BitTorrent protocol.
 */
public class BitTorrentProtocolViolationException extends RuntimeException {

	public BitTorrentProtocolViolationException(String message) {
		super(message);
	}

	public BitTorrentProtocolViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}
