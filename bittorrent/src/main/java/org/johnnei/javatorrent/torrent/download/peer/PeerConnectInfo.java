package org.johnnei.javatorrent.torrent.download.peer;

import java.net.InetSocketAddress;

import org.johnnei.javatorrent.torrent.download.Torrent;

/**
 * A small class which holds information about the peer which is trying to connect
 * @author Johnnei
 *
 */
public class PeerConnectInfo {
	
	/**
	 * If known the torrent to which this peer tries to connect
	 */
	private final Torrent torrent;
	
	/**
	 * The address information of this peer
	 */
	private final InetSocketAddress address;
	
	public PeerConnectInfo(Torrent torrent, InetSocketAddress address) {
		this.torrent = torrent;
		this.address = address;
	}
	
	public PeerConnectInfo(InetSocketAddress address) {
		this(null, address);
	}
	
	public Torrent getTorrent() {
		return torrent;
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}

}
