package org.johnnei.javatorrent.torrent;

/**
 * Extra exception class to filter torrent logic expections from actual exceptions
 * 
 * @author Johnnei
 * 
 */
public class TorrentException extends Exception {

	public TorrentException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
