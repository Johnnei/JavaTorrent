package org.johnnei.javatorrent.torrent;

/**
 * An exception class which allows for checked exception which cannot be gracefully handled within the Torrent logic to be thrown.
 */
public class TorrentException extends RuntimeException {

	static final long serialVersionUID = 1L;

	/**
	 * Creates a new torrent exception.
	 * @param message The reason this is fatal to the torrent logic.
	 * @param cause The causing exception.
	 */
	public TorrentException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new torrent exception.
	 * @param message The reason this is fatal to the torrent logic.
	 */
	public TorrentException(String message) {
		super(message);
	}
}
