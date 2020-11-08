package org.johnnei.javatorrent;

public interface TorrentClientSettings {

	/**
	 * @return <code>true</code> when the torrent client is allow to accept remote connections.
	 * Otherwise <code>false</code>
	 */
	boolean isAcceptingConnections();

	/**
	 * @return The port on which the client should be listening for connections.
	 */
	int getAcceptingPort();

}
