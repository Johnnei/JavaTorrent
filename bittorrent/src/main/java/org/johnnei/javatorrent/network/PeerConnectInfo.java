package org.johnnei.javatorrent.network;

import java.net.InetSocketAddress;
import java.util.Objects;

import org.johnnei.javatorrent.torrent.Torrent;

/**
 * A small class which holds information about the peer which is trying to connect
 * @author Johnnei
 *
 */
public class PeerConnectInfo {

	/**
	 * If known: the torrent to which this peer tries to connect
	 */
	private final Torrent torrent;

	/**
	 * The address information of this peer
	 */
	private final InetSocketAddress address;

	public PeerConnectInfo(Torrent torrent, InetSocketAddress address) {
		this.torrent = Objects.requireNonNull(torrent, "Torrent can not be null");
		this.address = Objects.requireNonNull(address, "Address can not be null");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + address.hashCode();
		result = prime * result + torrent.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PeerConnectInfo)) {
			return false;
		}
		PeerConnectInfo other = (PeerConnectInfo) obj;

		if (!Objects.equals(address, other.address)) {
			return false;
		}

		if (!Objects.equals(torrent, other.torrent)) {
			return false;
		}
		return true;
	}

	public Torrent getTorrent() {
		return torrent;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

}
