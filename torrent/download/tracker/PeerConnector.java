package torrent.download.tracker;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.johnnei.utils.ThreadUtils;

import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.encoding.SHA1;
import torrent.protocol.BitTorrent;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.IMessage;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.extension.MessageHandshake;

public class PeerConnector implements Runnable {

	private final Object LOCK_PEER_LIST = new Object();
	
	/**
	 * The object on which the thread will start sleeping once it is out of work
	 */
	public final Object PEER_JOB_NOTIFY = new Object();
	
	/**
	 * List of peer that are currently being connected
	 */
	private LinkedList<Peer> peers;
	
	private Manager manager;
	
	private final int maxPeers;
	
	public PeerConnector(Manager manager, int maxConnecting) {
		this.manager = manager;
		this.maxPeers = maxConnecting;
		peers = new LinkedList<>();
	}

	/**
	 * Adds a pending connection peer to the connection cycle
	 * 
	 * @param peer The peer to connect
	 */
	public void addPeer(Peer peer) {
		if (peers.size() >= maxPeers) {
			throw new IllegalStateException(String.format("Connector is full"));
		}
		
		synchronized (LOCK_PEER_LIST) {
			peers.add(peer);
		}
	}

	public void run() {
		while (true) {
			
			if (peers.isEmpty()) {
				ThreadUtils.wait(PEER_JOB_NOTIFY);
			}
			
			Peer peer = null;
			
			synchronized (LOCK_PEER_LIST) {
				peer = peers.remove();
			}
			
			if (peer == null) {
				continue;
			}
			
			try {
				peer.connect();
				
				if (peer.closed()) {
					continue;
				}
				
				peer.sendHandshake(manager.getTrackerManager().getPeerId());
				
				long timeWaited = 0;
				while (!peer.canReadMessage() && timeWaited < 10_000) {
					final int INTERVAL = 100;
					ThreadUtils.sleep(INTERVAL);
					timeWaited += INTERVAL;
				}
				
				if (!peer.canReadMessage()) {
					throw new IOException("Handshake timeout");
				}
				
				checkHandshake(peer);
				
				if (peer.getClient().supportsExtention(5, 0x10)) {
					// Extended Messages extension
					sendExtendedMessages(peer);
				}
				
				sendHaveMessages(peer);
				
			} catch (IOException e) {
				System.err.println(String.format("[PeerConnector] Failed to connect peer: %s", e.getMessage()));
				peer.close();
			}
		}
	}
	
	private void checkHandshake(Peer peer) throws IOException {
		BitTorrentHandshake handshake = peer.readHandshake();
		
		if (SHA1.match(peer.getTorrent().getHashArray(), handshake.getTorrentHash())) {
			throw new IOException("Peer is not downloading expected torrent");
		}
	}
	
	private void sendExtendedMessages(Peer peer) throws IOException {
		MessageExtension message;
		
		if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
			message = new MessageExtension(
				BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, 
				new MessageHandshake()
			);
		} else {
			message = new MessageExtension(
				BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, 
				new MessageHandshake(
					peer.getTorrent().getFiles().getMetadataSize()
				)
			);
		}
		
		peer.addToQueue(message);
	}
	
	private void sendHaveMessages(Peer peer) throws IOException {
		if (peer.getTorrent().getDownloadStatus() != Torrent.STATE_DOWNLOAD_DATA) {
			return;
		}
		
		Collection<IMessage> messages = peer.getTorrent().getFiles().getBitfield().getBitfieldMessage();
		for (IMessage message : messages) {
			peer.addToQueue(message);
		}
	}

	public int getFreeCapacity() {
		return maxPeers - peers.size();
	}

	public int getConnectingCount() {
		return peers.size();
	}

	public int getMaxCapacity() {
		return maxPeers;
	}

	@SuppressWarnings("unchecked")
	public int getConnectingCountFor(Torrent torrent) {
		LinkedList<Peer> peerList = null;
		
		synchronized (LOCK_PEER_LIST) {
			peerList = (LinkedList<Peer>) peers.clone();
		}
		
		return (int) peerList.stream().
				filter(p -> p.getTorrent().equals(torrent)).
				count();
	}

}
