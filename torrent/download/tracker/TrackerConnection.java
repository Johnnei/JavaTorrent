package torrent.download.tracker;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.PeerConnectInfo;
import torrent.network.Stream;

public class TrackerConnection {

	public static final int ACTION_CONNECT = 0;
	public static final int ACTION_ANNOUNCE = 1;
	public static final int ACTION_SCRAPE = 2;
	public static final int ACTION_ERROR = 3;
	public static final int ACTION_TRANSACTION_ID_ERROR = 256;
	
	public static final int EVENT_NONE = 0; 
	public static final int EVENT_COMPLETED = 1;
	public static final int EVENT_STARTED = 2;
	public static final int EVENT_STOPPED = 3;
	
	public static final long NO_CONNECTION_ID = 0x41727101980L;

	public static final String ERROR_CONNECTION_ID = "Connection ID missmatch.";
	
	private Logger log;

	private InetAddress address;
	private String name;
	private int port;
	private DatagramSocket socket;
	private Stream stream;

	private long connectionId;
	private int action;

	private String status;
	
	/**
	 * The pool of {@link PeerConnector} which will connect peers for us
	 */
	private PeerConnectorPool connectorPool;
	
	private TrackerManager manager;

	public TrackerConnection(Logger log, String url, PeerConnectorPool connectorPool, TrackerManager manager) {
		this.connectorPool = connectorPool;
		this.manager = manager;
		this.log = log;
		stream = new Stream();
		connectionId = NO_CONNECTION_ID;
		String[] urlData = url.split(":");
		if (!urlData[0].equals("udp")) {
			System.err.println("Only UDP trackers are supported: " + url);
		} else {
			try {
				name = urlData[1] + ":" + urlData[2];
				address = InetAddress.getByName(urlData[1].substring(2));
				port = Integer.parseInt(urlData[2].split("/")[0]);
				status = "Waiting";
			} catch (Exception e) {
				address = null;
				status = "Failed to parse address";
				log.warning(String.format("Failed to resolve tracker: %s", e.getMessage()));
			}
		}
	}

	public boolean isConnected() {
		return connectionId != NO_CONNECTION_ID;
	}

	public void connect() throws TrackerException {
		setStatus("Connecting");
		stream.reset(1000);
		int transactionId = manager.getTransactionId();
		stream.writeLong(connectionId);
		stream.writeInt(ACTION_CONNECT);
		stream.writeInt(transactionId);
		try {
			if (socket == null) {
				socket = new DatagramSocket();
				socket.setSoTimeout(15000);
			}
			socket.send(stream.write(address, port));
			stream.read(socket);
			action = stream.readInt();
			if (stream.readInt() != transactionId) {
				action = ACTION_TRANSACTION_ID_ERROR;
			}
			if (action != ACTION_CONNECT) {
				String error = stream.readString(stream.available());
				setStatus("Connection failed");
				throw new TrackerException("Tracker responded with an error: " + error);
			} else {
				connectionId = stream.readLong();
				setStatus("Connected");
			}
		} catch (IOException e) {
			socket = null;
			setStatus("Connection failed");
			throw new TrackerException("Tracker Packet got lost");
		}
	}

	/**
	 * Announces the torrent to the tracker and returns the announceInterval
	 * @param torrentInfo the torrent to announce
	 * @return The interval report by tracker or {@link Tracker#DEFAULT_ANNOUNCE_INTERVAL} on error
	 */
	public int announce(TorrentInfo torrentInfo) throws TrackerException {
		int connectorCapacity = connectorPool.getFreeCapacity();
		if (connectorCapacity == 0) {
			log.info("Ignored announce, connector is full.");
			return (int) TimeUnit.SECONDS.toMillis(30);
		}
		
		Torrent torrent = torrentInfo.getTorrent();
		setStatus("Announcing");
		stream.reset(100);
		int transactionId = manager.getTransactionId();
		stream.writeLong(connectionId);
		stream.writeInt(ACTION_ANNOUNCE);
		stream.writeInt(transactionId);
		stream.writeByte(torrent.getHashArray());
		stream.writeByte(manager.getPeerId());
		stream.writeLong(torrent.getDownloadedBytes()); // Downloaded Bytes
		stream.writeLong(torrent.getFiles().getRemainingBytes()); // Bytes left
		stream.writeLong(torrent.getUploadedBytes()); // Uploaded bytes
		int event = torrentInfo.getEvent();
		stream.writeInt(event);
		if(event != EVENT_NONE) {
			torrentInfo.setEvent(EVENT_NONE);
		}
		stream.writeInt(0); // Use sender ip
		stream.writeInt(new Random().nextInt());
		stream.writeInt(Math.min(connectorCapacity, torrent.peersWanted())); // Use defaults num_want (-1) Use the max our buffer can hold
		stream.writeShort(Config.getConfig().getInt("download-port"));
		stream.writeShort(0); // No extensions
		try {
			socket.send(stream.write(address, port));
			stream.read(socket);
			action = stream.readInt();
			if (transactionId != stream.readInt())
				action = ACTION_TRANSACTION_ID_ERROR;
			if (action != ACTION_ANNOUNCE) {
				String error = stream.readString(stream.available());
				log.warning(String.format("Announce failed with error: %d, Message: %s", action, error));
				setStatus("Announce failed");
				handleError(error);
				throw new TrackerException("Tracker responded with an error: " + error);
			}
			int announceInterval = stream.readInt();
			int leechers = stream.readInt();
			int seeders = stream.readInt();
			torrentInfo.setInfo(seeders, leechers);
			while (stream.available() >= 6) {
				byte[] address = stream.readIP();
				int port = stream.readShort();
				if (isEmptyIP(address))
					continue;
				
				InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(address), port);
				PeerConnectInfo peerInfo = new PeerConnectInfo(torrent, socketAddress);
				
				connectorPool.addPeer(peerInfo);
			}
			setStatus("Announced");
			return announceInterval;
		} catch (IOException e) {
			setStatus("Announce failed");
			throw new TrackerException("Tracker Packet got lost");
		}
	}

	public void scrape(TorrentInfo torrentInfo) throws TrackerException {
		Torrent torrent = torrentInfo.getTorrent();
		setStatus("Scraping");
		int transactionId = manager.getTransactionId();
		stream.reset(36);
		stream.writeLong(connectionId);
		stream.writeInt(ACTION_SCRAPE);
		stream.writeInt(transactionId);
		stream.writeByte(torrent.getHashArray());
		try {
			socket.send(stream.write(address, port));
			stream.read(socket);
			action = stream.readInt();
			if (stream.readInt() != transactionId) {
				action = ACTION_TRANSACTION_ID_ERROR;
			} if (action == ACTION_SCRAPE) {
				int seeders = stream.readInt();
				int downloaded = stream.readInt();
				int leechers = stream.readInt();
				torrentInfo.setInfo(seeders, leechers, downloaded);
				setStatus("Scraped");
			} else {
				String error = stream.readString(stream.available());
				setStatus("Scrape failed");
				handleError(error);
				throw new TrackerException("Tracker responded with an error: " + error);
			}
		} catch (IOException e) {
			throw new TrackerException("Tracker Packet got lost");
		}
	}

	private void handleError(String error) {
		if (error.startsWith(ERROR_CONNECTION_ID)) {
			connectionId = NO_CONNECTION_ID;
		}
	}

	private boolean isEmptyIP(byte[] address) {
		for (int i = 0; i < address.length; i++) {
			if (address[i] != 0)
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return address.getHostAddress();
	}

	private void setStatus(String s) {
		status = s;
	}

	public String getStatus() {
		return status;
	}
	
	public InetAddress getAddress() {
		return address;
	}

	public String getTrackerName() {
		return name;
	}

}
