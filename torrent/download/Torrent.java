package torrent.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import torrent.Logable;
import torrent.download.algos.BurstPeerManager;
import torrent.download.algos.FullPieceSelect;
import torrent.download.algos.IDownloadRegulator;
import torrent.download.algos.IPeerManager;
import torrent.download.files.PieceInfo;
import torrent.download.peer.Peer;
import torrent.download.tracker.Tracker;
import torrent.protocol.IMessage;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.MessageInterested;
import torrent.protocol.messages.MessageUninterested;
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
	 * Contains the metadata of this torrent
	 */
	private Metadata metadata;
	/**
	 * Contains all data of the actual torrent
	 */
	private TorrentFiles torrentFiles;
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

	public static final byte STATE_DOWNLOAD_METADATA = 0;
	public static final byte STATE_DOWNLOAD_DATA = 1;

	/**
	 * The default request size of 16KB
	 */
	public static final int REQUEST_SIZE = 1 << 15;

	/**
	 * Creates a torrent with space for 10 trackers
	 */
	public Torrent() {
		this(10);
	}

	public Torrent(int trackerCount) {
		trackers = new Tracker[trackerCount];
		downloadedBytes = 0L;
		peers = new ArrayList<Peer>();
		keepDownloading = true;
		metadata = new Metadata();
		status = "Parsing Magnet Link";
		downloadRegulator = new FullPieceSelect(this);
		peerManager = new BurstPeerManager(50, 1.5F);
		System.setOut(new Logger(System.out));
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
			log("Filtered duplicate Peer: " + p, true);
			return;
		}
		if (peersWanted() > 0) {
			p.start();
			synchronized (this) {
				peers.add(p);
			}
		} else {
			p.close();
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

	public void run() {
		downloadTorrentFile();
		downloadFiles();
	}

	private void downloadFiles() {
		torrentFiles = new TorrentFiles(new File("./" + displayName + ".torrent"));
		torrentStatus = STATE_DOWNLOAD_DATA;

		log("Phase 2: Downloading torrent files");
		log("Download Regulator: " + downloadRegulator.getName());
		log("Peer Manager: " + peerManager.getName());
		
		for(int i = 0; i < torrentFiles.getFiles().length; i++) {
			FileInfo fInfo = torrentFiles.getFiles()[i];
			try {
				fInfo.getPieceWriter().reserveDiskspace(fInfo.getSize());
			} catch (IOException e) {
				
			}
		}

		long lastPeerCheck = System.currentTimeMillis();
		long lastPeerUpdate = System.currentTimeMillis();

		while (!torrentFiles.hasAllPieces()) {
			long startTime = System.currentTimeMillis();
			if (System.currentTimeMillis() - lastPeerUpdate > 10000) {
				updatePeers();
				lastPeerUpdate = System.currentTimeMillis();
			}
			if (System.currentTimeMillis() - lastPeerCheck > 5000) {
				checkPeers();
				lastPeerCheck = System.currentTimeMillis();
			}
			synchronized (this) {
				PieceInfo piece = downloadRegulator.getPiece();
				if (piece != null) {
					ArrayList<Peer> peerList = downloadRegulator.getPeerForPiece(piece);
					for (int i = 0; i < peerList.size() && !torrentFiles.getPiece(piece.getIndex()).isRequestedAll(); i++) {
						Peer p = peerList.get(i);
						int requestAmount = p.getFreeWorkTime();
						while(requestAmount-- > 0 && !torrentFiles.getPiece(piece.getIndex()).isRequestedAll()) {
							p.requestPiece(torrentFiles.getPiece(piece.getIndex()));
						}
					}
				}
			}
			int sleep = (int) (25 - (System.currentTimeMillis() - startTime));
			if (sleep > 0) {
				sleep(sleep);
			}
		}
	}

	private void downloadTorrentFile() {
		System.out.println("Initiating Download");
		for (Tracker t : trackers) {
			if (t != null) {
				if(!t.isAlive())
					t.start();
			}
		}
		log("Phase 1: Downloading " + displayName + ".torrent");
		while (!metadata.hasAllPieces()) {
			if (metadata.hasMetainfo()) {
				int index = metadata.getNextPieceIndex();
				if (index > -1) { // Check if we still need a piece
					synchronized (this) {
						Random r = new Random();
						for (int i = r.nextInt(peers.size()); i < peers.size(); i++) {
							Peer p = peers.get(i);
							if (p.hasExtentionId("ut_metadata") && !p.isWorking()) {
								p.requestMetadataPiece(index);
								metadata.requestedPiece(index, true);
								break;
							}
						}
					}
				}
			}
			sleep(100);
		}
		log("Recieved all pieces, Checking hash");
		if (metadata.checkHash(btihHash)) {
			log("Hash matched, Saving file to disk");
			metadata.save(new File("./" + displayName + ".torrent"));
		} else {
			log("Hash failed, Redownloading metadata");
			metadata.clear();
			downloadTorrentFile();
		}
	}

	/**
	 * Checks if the peers are still connected
	 */
	private synchronized void checkPeers() {
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p.closed()) {
				peers.remove(i--);
				continue;
			}
		}
	}

	/**
	 * Updates the interested states
	 */
	private synchronized void updatePeers() {
		ArrayList<PieceInfo> neededPieces = torrentFiles.getNeededPieces();
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			boolean hasNoPieces = true;
			for (int j = 0; j < neededPieces.size(); j++) {
				if (p.hasPiece(neededPieces.get(j).getIndex())) {
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
		}
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
		String s = "";
		for (int i = 0; i < btihHash.length; i++) {
			s += Integer.toHexString(btihHash[i] & 0xff);
		}
		return s;
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

	public Metadata getMetadata() {
		return metadata;
	}

	public int getPieceSize(int index, boolean isMetadata) {
		return (isMetadata) ? metadata.getPieceSize(index) : -1;
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
		for(int i = 0; i < torrentFiles.getPieceCount(); i++) {
			progress += torrentFiles.getPiece(i).getProgress();
		}
		return 100D * (progress / (torrentFiles.getPieceCount() * 100));
	}

	/**
	 * Cancels the piece<br/>
	 * It will re-add this piece to the requestable pieces pool
	 * @param index
	 * if negative it will be a metadata piece else a normal piece
	 * @param piece
	 */
	public void cancelPiece(int index, int piece) {
		if(index < 0) {
			metadata.getPiece(-(index + 1)).setRequested(false);
		} else {
			torrentFiles.getPiece(index).cancel(piece);
		}
	}

	public void collectPiece(int index, int offset, byte[] data) {
		synchronized (this) {
			downloadedBytes += data.length;
			torrentFiles.fillPiece(index, offset, data);
			log("Received Piece: " + index + "-" + (offset / REQUEST_SIZE));
			if (torrentFiles.getPiece(index).hasAllBlocks()) {
				if (torrentFiles.getPiece(index).checkHash()) {
					try {
						torrentFiles.save(index);
						torrentFiles.getPiece(index).reset();
						broadcastMessage(new MessageHave(index));
						log("Recieved and verified piece: " + index);
						String p = Double.toString(getProgress());
						log("Torrent Progress: " + p.substring(0, (p.length() < 4) ? p.length() : 4) + "%");
					} catch (IOException e) {
						log("Saving piece " + index + " failed: " + e.getMessage(), true);
					}
				} else {
					log("Hash check failed on piece: " + index, true);
					torrentFiles.getPiece(index).reset();
				}
			}
		}
	}

	public void collectPiece(int index, byte[] data) {
		if (data == null) {
			log("Retrieved Error on piece: " + index, true);
			metadata.requestedPiece(index, false);
		} else {
			log("Retrieved Piece " + index);
			metadata.fillPiece(index, data);
		}
	}

	public synchronized void broadcastMessage(IMessage m) {
		for (int i = 0; i < peers.size(); i++) {
			if (!peers.get(i).closed())
				peers.get(i).addToQueue(m);
		}
	}

	@Override
	public String toString() {
		return StringUtil.byteArrayToString(btihHash);
	}

	public boolean needAnnounce() {
		return peersWanted() > 0 && peers.size() < peerManager.getMaxPendingPeers(torrentStatus);
	}

	public int getMaxPeers() {
		return peerManager.getMaxPeers(torrentStatus);
	}

	public int peersWanted() {
		return peerManager.getAnnounceWantAmount(torrentStatus, getSeedCount() + getLeecherCount());
	}

	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	public void pollRates() {
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				peers.get(i).pollRates();
			}
		}
	}

	public TorrentFiles getTorrentFiles() {
		return torrentFiles;
	}

	public int getDownloadRate() {
		int dlRate = 0;
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				dlRate += peers.get(i).getDownloadRate();
			}
		}
		return dlRate;
	}

	public int getUploadRate() {
		int ulRate = 0;
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				ulRate += peers.get(i).getUploadRate();
			}
		}
		return ulRate;
	}
	
	public int getConnectingCount() {
		return peers.size() - getSeedCount() - getLeecherCount();
	}

	public int getSeedCount() {
		int seeds = 0;
		if (torrentFiles == null || peers == null)
			return 0;
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				if (peers.get(i).getClient().hasPieceCount() == torrentFiles.getPieceCount())
					++seeds;
			}
		}
		return seeds;
	}

	public int getLeecherCount() {
		int leechers = 0;
		synchronized (this) {
			for (int i = 0; i < peers.size(); i++) {
				if (peers.get(i).getPassedHandshake())
					leechers++;
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
	
	public long getRemainingBytes() {
		if(torrentStatus == STATE_DOWNLOAD_DATA)
			return torrentFiles.getRemainingBytes();
		else
			return metadata.getRemainingBytes();
	}

	/**
	 * Gets all lecheers which: has atleast 1 piece, does not have all pieces and has us unchoked
	 * @return
	 */
	public ArrayList<Peer> getDownloadableLeechers() {
		ArrayList<Peer> leechers = new ArrayList<Peer>();
		synchronized(this) {
			for(Peer p : peers) {
				if(p.hasPieceCount() > 0 && p.hasPieceCount() != torrentFiles.getPieceCount() && !p.getMyClient().isChoked())
					leechers.add(p);
			}
		}
		return leechers;
	}

}
