package org.johnnei.javatorrent.bittorrent.tracker;

public class TrackerException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public TrackerException(String message, Throwable cause) {
		super(message, cause);
	}

	public TrackerException(String message) {
		super(message);
	}

}
