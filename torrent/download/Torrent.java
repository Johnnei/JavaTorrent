package torrent.download;

import java.io.File;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;

import torrent.Logable;
import torrent.Manager;
import torrent.TorrentException;
import torrent.download.algos.BurstPeerManager;
import torrent.download.algos.FullPieceSelect;
import torrent.download.algos.IDownloadRegulator;
import torrent.download.algos.IPeerManager;
import torrent.download.files.Block;
import torrent.download.files.Piece;
import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.download.tracker.PeerConnectorThread;
import torrent.download.tracker.Tracker;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.MessageChoke;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.MessageInterested;
import torrent.protocol.messages.MessageUnchoke;
import torrent.protocol.messages.MessageUninterested;
import torrent.protocol.messages.extention.MessageExtension;
import torrent.protocol.messages.ut_metadata.MessageRequest;
import torrent.util.Logger;
import torrent.util.StringUtil;

public class Torrent extends Thread implements Logable {

	/**
	 * The display name of this torrent
	 */
	private String displayName;
	/**
	 * The list of trackers
	 */
	private Tracker[] trackers;
	/**
	 * The SHA1 hash from the magnetLink
	 */
	private byte[] btihHash;
	/**
	 * All connected peers
	 */
	private ArrayList<Peer> peers;
	private boolean keepDownloading;
	/**
	 * The current status
	 */
	private String status;
	/**
	 * Contains all data of the actual torrent
	 */
	private Files files;
	/**
	 * The current state of the torrent
	 */
	private byte torrentStatus;
	/**
	 * Regulates the selection of pieces and the peers to download the pieces
	 */
	private IDownloadRegulator downloadRegulator;
	/**
	 * Regulates the connection with peers
	 */
	private IPeerManager peerManager;
	/**
	 * The amount of downloaded bytes
	 */
	private long downloadedBytes;
	/**
	 * The last time all peer has been checked for disconnects
	 */
	private long lastPeerCheck = System.currentTimeMillis();
	/**
	 * The last time all peer interest states have been updated
	 */
	private long lastPeerUpdate = System.currentTimeMillis();
	/**
	 * Remembers if the torrent is collecting a piece so we can wait until all pieces are written to the hdd before continueing
	 */
	private int collectingPiece;
	/**
	 * The threads which connects new peers
	 */
	private PeerConnectorThread[] connectorThreads;
	/**
	 * The thread which reads all information from the peers
	 */
	private PeersReadThread readThread;
	/**
	 * The thread which writes all information to the peers
	 */
	private PeersWriteThread writeThread;

	public static final byte STATE_DOWNLOAD_METADATA = 0;
	public static final byte STATE_DOWNLOAD_DATA = 1;

	/**
	 * Creates a torrent with space for 10 trackers
	 */
	public Torrent() {
		this(10);
	}

	public Torrent(int trackerCount) {
		trackers = new Tracker[trackerCount];
		torrentStatus = STATE_DOWNLOAD_METADATA;
		downloadedBytes = 0L;
		peers = new ArrayList<Peer>();
		keepDownloading = true;
		status = "Parsing Magnet Link";
		downloadRegulator = new FullPieceSelect(this);
		peerManager = new BurstPeerManager(500, 1.5F);
		System.setOut(new Logger(System.out));
		System.setErr(new Logger(System.err));
	}

	private boolean hasPeer(Peer p) {
		synchronized (this) {
			for (Peer _p : peers) {
				if (_p != null) {
					if (_p.toString().equals(p))
						return true;
				}
			}
		}
		return false;
	}

	public void addPeer(Peer p) {
		if (hasPeer(p)) {
			p.close();
			log("Filtered duplicate Peer: " + p, true);
			return;
		}
		synchronized (this) {
			peers.add(p);
		}
	}

	public void addTracker(Tracker t) {
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] == null) {
				trackers[i] = t;
				return;
			}
		}
		System.err.println("Failed to add tracker to " + getDisplayName());
	}
	
	public void initialise() {
		Manager.getManager().addTorrent(this);
		connectorThreads = new PeerConnectorThread[4];
		for(int i = 0; i <connectorThreads.length; i++) {
			connectorThreads[i] = new PeerConnectorThread(this, 50);
			connectorThreads[i].start();
		}
		readThread = new PeersReadThread(this);
		writeThread = new PeersWriteThread(this);
		readThread.start();
		writeThread.start();
		for(int i = 0; i < trackers.length; i++) {
			Tracker t = trackers[i];
			if(t != null) {
				t.start();
			}
		}
	}

	public void run() {
		while(!files.isDone() || collectingPiece > 0) {
			processPeers();
			ArrayList<Peer> downloadPeers = getDownloadablePeers();
			while(downloadPeers.size() > 0) {
				Peer peer = downloadPeers.remove(0);
				Piece piece = downloadRegulator.getPieceForPeer(peer);
				if(piece == null) {
					continue;
				}
				while(piece.getRequestedCount() < piece.getBlockCount() && peer.getFreeWorkTime() > 0) {
					Block block = piece.getRequestBlock();
					if(block == null) {
						break;
					} else {
						IMessage message = null;
						if(files.isMetadata()) {
							message = new MessageExtension(peer.getClient().getExtentionID(UTMetadata.NAME), new MessageRequest(block.getIndex()));
						} else {
							message = new torrent.protocol.messages.MessageRequest(piece.getIndex(), block.getIndex() * files.getBlockSize(), block.getSize());
						}
						peer.getMyClient().addJob(new Job(piece.getIndex(), block.getIndex()));
						peer.addToQueue(message);
					}
				}
			}
			ThreadUtils.sleep(25);
		}
		if(files.isMetadata()) {
			FileInfo f = files.getFiles()[0];
			log("Metadata download completed, Starting phase 2");
			files = new Files(new File(f.getFilename()));
			torrentStatus = STATE_DOWNLOAD_DATA;
			run();
		} else {
			log("Completed download, Switching to upload mode!");
			//TODO Upload Mode
		}
		log("loop ended", true);
	}
	
	/**
	 * Manages all states about peers
	 */
	private void processPeers() {
		if (System.currentTimeMillis() - lastPeerUpdate > 20000) {
			updatePeers();
			lastPeerUpdate = System.currentTimeMillis();
		}
		if (System.currentTimeMillis() - lastPeerCheck > 5000) {
			cleanPeerList();
			lastPeerCheck = System.currentTimeMillis();
		}
	}

	/**
	 * Checks if the peers are still connected
	 */
	private void cleanPeerList() {
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p == null)
				continue;
			if (p.closed()) {
				p.cancelAllPieces();
				synchronized (this) {
					peers.remove(i--);
				}
				continue;
			} else {
				p.checkDisconnect();
			}
		}
	}

	/**
	 * Updates the interested states
	 */
	private void updatePeers() {
		if(files == null)
			return;
		ArrayList<Piece> neededPieces = files.getNeededPieces();
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			boolean hasNoPieces = true;
			for (int j = 0; j < neededPieces.size(); j++) {
				if (p.getClient().hasPiece(neededPieces.get(j).getIndex())) {
					hasNoPieces = false;
					if (!p.getClient().isInterested()) {
						p.addToQueue(new MessageInterested());
						p.getClient().interested();
					}
					break;
				}
			}
			if (hasNoPieces && p.getClient().isInterested()) {
				p.addToQueue(new MessageUninterested());
				p.getClient().uninterested();
			}
			if (p.getMyClient().isInterested() && p.getClient().isChoked()) {
				p.addToQueue(new MessageUnchoke());
				p.getClient().unchoke();
			} else if (!p.getMyClient().isInterested() && !p.getClient().isChoked()) {
				p.addToQueue(new MessageChoke());
				p.getClient().choke();
			}
		}
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
		files = new Files("./" + displayName + ".torrent");
		setName(displayName);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setHash(byte[] i) {
		btihHash = i;
	}

	public boolean hasHash() {
		return btihHash != null;
	}

	public boolean hasTracker() {
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] != null)
				return true;
		}
		return false;
	}

	public byte[] getHashArray() {
		return btihHash;
	}

	public String getHash() {
		return StringUtil.byteArrayToString(btihHash);
	}

	public void log(String s, boolean error) {
		s = "[" + toString() + "] " + s;
		if (error)
			System.err.println(s);
		else
			System.out.println(s);
	}

	public void log(String s) {
		log(s, false);
	}

	public boolean keepDownloading() {
		return keepDownloading;
	}

	@Override
	public String getStatus() {
		return status;
	}

	public byte getDownloadStatus() {
		return torrentStatus;
	}

	public double getProgress() {
		double progress = 0D;
		for(int i = 0; i < files.getPieceCount(); i++) {
			Piece p = files.getPiece(i);
			progress += 100 * (p.getDoneCount() / (double)p.getBlockCount());
		}
		return 100D * (progress / (files.getPieceCount() * 100));
	}

	public void collectPiece(int index, int offset, byte[] data) {
		synchronized(this) {
			++collectingPiece;
		}
		int blockIndex = offset / files.getBlockSize();
		try {
			files.getPiece(index).storeBlock(blockIndex, data);
			if(!files.isMetadata()) {
				downloadedBytes += data.length;
			}
			if (files.getPiece(index).isDone()) {
				if (files.getPiece(index).checkHash()) {
					if(torrentStatus == STATE_DOWNLOAD_DATA)
						broadcastMessage(new MessageHave(index));
					log("Recieved and verified piece: " + index);
					String p = Double.toString(getProgress());
					log("Torrent Progress: " + p.substring(0, (p.length() < 4) ? p.length() : 4) + "%");
				} else {
					log("Hash check failed on piece: " + index, true);
					files.getPiece(index).hashFail();
				}
			}
		} catch (TorrentException e) {
			log(e.getMessage(), true);
			files.getPiece(index).reset(blockIndex);
		}
		synchronized(this) {
			--collectingPiece;
		}
	}

	public void broadcastMessage(IMessage m) {
		for (int i = 0; i < peers.size(); i++) {
			if (!peers.get(i).closed())
				peers.get(i).addToQueue(m);
		}
	}

	@Override
	public String toString() {
		return StringUtil.byteArrayToString(btihHash);
	}
	
	private int getFreeConnectingCapacity() {
		int capacity = 0;
		for(int i = 0; i < connectorThreads.length; i++) {
			capacity += connectorThreads[i].getFreeCapacity();
		}
		return capacity;
	}

	public boolean needAnnounce() {
		return peersWanted() > 10 && peers.size() < peerManager.getMaxPendingPeers(torrentStatus) && getFreeConnectingCapacity() > 0;
	}

	public int getMaxPeers() {
		return peerManager.getMaxPeers(torrentStatus);
	}

	public int peersWanted() {
		
		return (int)Math.min(peerManager.getAnnounceWantAmount(torrentStatus, getSeedCount() + getLeecherCount()), getFreeConnectingCapacity());
	}

	public void pollRates() {
		for (int i = 0; i < peers.size(); i++) {
			peers.get(i).pollRates();
		}
	}

	public Files getFiles() {
		return files;
	}
	
	public PeerConnectorThread getConnectorThread() {
		for(int i = 0; i < connectorThreads.length; i++) {
			if(connectorThreads[i].getFreeCapacity() > 0)
				return connectorThreads[i];
		}
		return null;
	}

	public int getDownloadRate() {
		int dlRate = 0;
		for (int i = 0; i < peers.size(); i++) {
			dlRate += peers.get(i).getDownloadRate();
		}
		return dlRate;
	}

	public int getUploadRate() {
		int ulRate = 0;
		for (int i = 0; i < peers.size(); i++) {
			ulRate += peers.get(i).getUploadRate();
		}
		return ulRate;
	}

	public int getSeedCount() {
		if(torrentStatus == STATE_DOWNLOAD_METADATA)
			return 0;
		int seeds = 0;
		if (files == null || peers == null)
			return 0;
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p == null)
				continue;
			if (p.getClient().hasPieceCount() == files.getPieceCount())
				++seeds;
		}
		
		return seeds;
	}

	public int getLeecherCount() {
		int leechers = 0;
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				Peer peer = peers.get(i);
				if (peer != null) {
					if (peer.getPassedHandshake())
						leechers++;
				}
			}
		}
		return leechers - getSeedCount();
	}

	public Tracker[] getTrackers() {
		return trackers;
	}

	public ArrayList<Peer> getPeers() {
		return peers;
	}
	
	/**
	 * The amount of bytes downloaded
	 * @return
	 */
	public long getDownloadedBytes() {
		return downloadedBytes;
	}

	/**
	 * Gets all lecheers which: has atleast 1 piece and has us unchoked
	 * @return
	 */
	public ArrayList<Peer> getDownloadablePeers() {
		ArrayList<Peer> leechers = new ArrayList<Peer>();
		for(int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if(p == null)
				continue;
			if(torrentStatus == STATE_DOWNLOAD_METADATA) {
				if(p.getClient().hasExtentionID(UTMetadata.NAME))
					leechers.add(p);
			} else {
				if(p.getClient().hasPieceCount() > 0 && !p.getMyClient().isChoked() && p.getFreeWorkTime() > 0)
					leechers.add(p);
			}
		}
		return leechers;
	}

	public int getConnectingCount() {
		int connecting = 0;
		for(int i = 0; i < connectorThreads.length; i++) {
			connecting += connectorThreads[i].getConnectingCount();
		}
		return connecting;
	}

}
