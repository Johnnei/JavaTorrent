package org.johnnei.javatorrent.internal.tracker.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnounceRequest implements IUdpTrackerPayload {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnounceRequest.class);

	// Request
	private final TorrentInfo torrentInfo;

	private final Torrent torrent;

	private final byte[] peerId;

	private final int downloadPort;

	// Response
	private Collection<SocketInfo> sockets;

	private int leechers;

	private int seeders;

	private int interval;

	public AnnounceRequest(TorrentInfo torrentInfo, byte[] peerId, int downloadPort) {
		this.torrentInfo = Objects.requireNonNull(torrentInfo);
		this.torrent = torrentInfo.getTorrent();
		this.peerId = Objects.requireNonNull(peerId);
		this.downloadPort = downloadPort;
		if (peerId.length != 20) {
			throw new IllegalArgumentException(String.format("Given peer ID is %d bytes instead of the expected 20.", peerId.length));
		}
		sockets = new ArrayList<>();
	}

	@Override
	public void writeRequest(OutStream outStream) {
		outStream.writeByte(torrent.getHashArray());
		outStream.writeByte(peerId);

		// Downloaded Bytes
		outStream.writeLong(torrent.getDownloadedBytes());

		// Bytes left (or 0 if we don't know how many we should download)
		if (torrent.getFiles() != null) {
			outStream.writeLong(torrent.getFiles().countRemainingBytes()); // Bytes left
		} else {
			outStream.writeLong(0);
		}
		outStream.writeLong(torrent.getUploadedBytes());

		// Write event code and reset it to NONE if it wasn't.
		TrackerEvent event = torrentInfo.getEvent();
		outStream.writeInt(event.getId());
		if(event != TrackerEvent.EVENT_NONE) {
			torrentInfo.setEvent(TrackerEvent.EVENT_NONE);
		}

		// Indicate that we want the response on the sending IP
		outStream.writeInt(0);
		// Really don't know what they 'key' field is used for
		outStream.writeInt(new Random().nextInt());

		// Request as much as we want for the torrent
		outStream.writeInt(torrent.peersWanted());
		outStream.writeShort(downloadPort);
		// No extensions as defined in BEP #41
		outStream.writeShort(0);
	}

	@Override
	public void readResponse(InStream inStream) {
		final int bytesPerPeer = 6;

		interval = inStream.readInt();
		leechers = inStream.readInt();
		seeders = inStream.readInt();

		if (inStream.available() % bytesPerPeer != 0) {
			LOGGER.warn(String.format("Peer information bytes aren't divisible by 6: %d", inStream.available()));
		}

		while (inStream.available() >= bytesPerPeer) {
			byte[] ip = inStream.readFully(4);
			int port = inStream.readUnsignedShort();
			sockets.add(new SocketInfo(ip, port));
		}
	}

	@Override
	public void process(UdpTracker tracker) {
		torrentInfo.setInfo(seeders, leechers);
		tracker.setAnnounceInterval(interval);
		sockets.stream()
			.filter(this::isValidSocket)
			.map(socket -> {
				InetSocketAddress socketAddress;
				try {
					socketAddress = new InetSocketAddress(InetAddress.getByAddress(socket.ip), socket.port);
				} catch (Exception e) {
					LOGGER.debug("Discarding invalid socket for peer", e);
					return Optional.empty();
				}
				return Optional.of(new PeerConnectInfo(torrent, socketAddress));
			})
			.filter(peer -> peer.isPresent())
			.map(peer -> (PeerConnectInfo) peer.get())
			.forEach(tracker::connectPeer);
	}

	private boolean isValidSocket(SocketInfo socket) {
		for (int i = 0; i < socket.ip.length; i++) {
			if (socket.ip[i] != 0) {
				return true;
			}
		}

		// IP is 0.0.0.0 which isn't valid
		return false;
	}

	@Override
	public TrackerAction getAction() {
		return TrackerAction.ANNOUNCE;
	}

	@Override
	public int getMinimalSize() {
		return 12;
	}

	private static final class SocketInfo {

		private final byte[] ip;

		private final int port;

		public SocketInfo(byte[] ip, int port) {
			this.ip = ip;
			this.port = port;
		}

	}

}
