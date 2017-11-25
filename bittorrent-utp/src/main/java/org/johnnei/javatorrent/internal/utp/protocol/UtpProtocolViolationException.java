package org.johnnei.javatorrent.internal.utp.protocol;

/**
 * An exception indicating that the uTP protocol has not been used correctly.
 */
public class UtpProtocolViolationException extends RuntimeException {

	public UtpProtocolViolationException(String message) {
		super(message);
	}
}
