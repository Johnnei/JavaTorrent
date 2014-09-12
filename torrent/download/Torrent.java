package torrent.download;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.Logable;
import torrent.TorrentManager;
import torrent.download.algos.BurstPeerManager;
import torrent.download.algos.FullPieceSelect;
import torrent.download.algos.IDownloadPhase;
import torrent.download.algos.IDownloadRegulator;
import torrent.download.algos.IPeerManager;
import torrent.download.algos.PhaseMetadata;
import torrent.download.files.Piece;
import torrent.download.files.disk.DiskJob;
import torrent.download.files.disk.DiskJobStoreBlock;
import torrent.download.files.disk.IOManager;
import torrent.download.peer.Peer;
import torrent.download.tracker.TrackerManager;
import torrent.encoding.SHA1;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.MessageChoke;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.MessageInterested;
import torrent.protocol.messages.MessageUnchoke;
import torrent.protocol.messages.MessageUninterested;
import torrent.util.StringUtil;

public class Torrent extends Thread implements Logable {

	/**
	 * The display name of this torrent
	 */
	private String displayName;

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
	 * The amount of uploaded bytes
	 */
	private long uploadedBytes;
	/**
	 * The last time all peer has been checked for disconnects
	 */
	private long lastPeerCheck = System.currentTimeMillis();
	/**
	 * The last time all peer interest states have been updated
	 */
	private long lastPeerUpdate = System.currentTimeMillis();
	/**
	 * Remembers if the torrent is collecting a piece or checking the hash so we can wait until all pieces are written to the hdd before continuing
	 */
	private int torrentHaltingOperations;

	/**
	 * IOManager to manage the transaction between the hdd and the programs so none of the actual network thread need to get block for that
	 */
	private IOManager ioManager;
	/**
	 * The phase in which the torrent currently is
	 */
	private IDownloadPhase phase;
	
	/**
	 * The manager which takes care of all torrents and trackers
	 */
	private TorrentManager manager;

	public static final byte STATE_DOWNLOAD_METADATA = 0;
	public static final byte STATE_DOWNLOAD_DATA = 1;
	public static final byte STATE_UPLOAD = 2;

	public Torrent(TorrentManager manager, TrackerManager trackerManager, byte[] btihHash, String displayName) {
		super(displayName);
		this.displayName = displayName;
		this.files = new Files("./" + displayName + ".torrent");
		this.manager = manager;
		this.btihHash = btihHash;
		torrentStatus = STATE_DOWNLOAD_METADATA;
		downloadedBytes = 0L;
		peers = new ArrayList<Peer>();
		keepDownloading = true;
		status = "Parsing Magnet Link";
		ioManager = new IOManager();
		downloadRegulator = new FullPieceSelect(this);
		peerManager = new BurstPeerManager(Config.getConfig().getInt("peer-max"), Config.getConfig().getFloat("peer-max_burst_ratio"));
		phase = new PhaseMetadata(trackerManager, this);
	}

	private boolean hasPeer(Peer peer) {
		synchronized (this) {
			return peers.stream().filter(p -> p.equals(peer)).findAny().isPresent();
		}
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

	/**
	 * Registers the torrent with the {@link TorrentManager}
	 */
	public void initialise() {
		manager.addTorrent(this);
	}

	/**
	 * Updates the bitfield size for all peers
	 */
	public void updateBitfield() {
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p != null) {
				p.getClient().getBitfield().setBitfieldSize(files.getBitfieldSize());
			}
		}
	}

	public void run() {
		while(phase != null) {
			torrentStatus = phase.getId();
			phase.preprocess();
			while (!phase.isDone() || torrentHaltingOperations > 0) {
				processPeers();
				phase.process();
				ioManager.processTask(this);
				ThreadUtils.sleep(25);
			}
			phase.postprocess();
			phase = phase.nextPhase();
		}
		log("Torrent has finished");
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
		if (files == null)
			return;
		ArrayList<Piece> neededPieces = files.getNeededPieces();
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			boolean hasNoPieces = true;
			for (int j = 0; j < neededPieces.size(); j++) {
				if (p.getClient().getBitfield().hasPiece(neededPieces.get(j).getIndex())) {
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

	public String getDisplayName() {
		return displayName;
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
		if (files.getPieceCount() == 0) {
			return 0D;
		}
		return 100D * (files.getBitfield().hasPieceCount() / (double) files.getPieceCount());
	}

	/**
	 * Tells the torrent to save a block of data
	 * 
	 * @param index The piece index
	 * @param offset The offset within the piece
	 * @param data The bytes to be stored
	 */
	public void collectPiece(int index, int offset, byte[] data) {
		addToHaltingOperations(1);
		int blockIndex = offset / files.getBlockSize();
		addDiskJob(new DiskJobStoreBlock(index, blockIndex, data));
	}

	/**
	 * Called by IOManager to notify the torrent that we processed the collectingPiece
	 */
	public void addToHaltingOperations(int i) {
		synchronized (this) {
			torrentHaltingOperations += i;
		}
	}

	/**
	 * Adds a task to the IOManager of this torrent
	 * 
	 * @param task The task to add
	 */
	public void addDiskJob(DiskJob task) {
		ioManager.addTask(task);
	}

	/**
	 * Broadcasts a have message to all peers and updates the have states for myClient
	 * 
	 * @param pieceIndex The index of the piece to be broadcasted
	 */
	public void broadcastHave(int pieceIndex) {
		MessageHave have = new MessageHave(pieceIndex);
		downloadedBytes -= files.getPiece(pieceIndex).getSize();
		files.havePiece(pieceIndex);
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (!p.closed() && p.getPassedHandshake()) {
				p.addToQueue(have);
			}
		}
	}

	public void broadcastMessage(IMessage m) {
		for (int i = 0; i < peers.size(); i++) {
			Peer peer = peers.get(i);
			if (!peer.closed() && peer.getPassedHandshake())
				peer.addToQueue(m);
		}
	}

	/**
	 * Calculates the current progress based on all available files on the HDD
	 */
	public void checkProgress() {
		updateBitfield();
		log("Checking progress...");
		FileInfo[] fileinfo = files.getFiles();
		for (int i = 0; i < fileinfo.length; i++) {
			FileInfo info = fileinfo[i];
			RandomAccessFile file = info.getFileAcces();
			try {
				if (file.length() > 0L) {
					int pieceIndex = (int) (info.getFirstByteOffset() / files.getPieceSize());
					int lastPieceIndex = pieceIndex + info.getPieceCount();
					for (; pieceIndex < lastPieceIndex; pieceIndex++) {
						try {
							if (files.getPiece(pieceIndex).checkHash()) {
								// log("Progress Check: Have " + pieceIndex);
								if (torrentStatus == STATE_DOWNLOAD_DATA) {
									broadcastHave(pieceIndex);
								} else {
									files.havePiece(pieceIndex);
								}

							}
						} catch (Exception e) {
						}
					}
				}
			} catch (IOException e) {
			}
		}
		log("Checking progress done");
	}
	
	/**
	 * Adds the amount of bytes to the uploaded count
	 * 
	 * @param l The amount of bytes to add
	 */
	public void addUploadedBytes(long l) {
		uploadedBytes += l;
	}
	
	public void setFiles(Files files) {
		this.files = files;
	}

	@Override
	public String toString() {
		return StringUtil.byteArrayToString(btihHash);
	}

	/**
	 * Checks if the tracker needs to announce
	 * 
	 * @return true if we need more peers
	 */
	public boolean needAnnounce() {
		int peersPerThread = Config.getConfig().getInt("peer-max_connecting") / Config.getConfig().getInt("peer-max_concurrent_connecting");
		boolean needEnoughPeers = peersWanted() >= peersPerThread;
		boolean notExceedMaxPendingPeers = peers.size() < peerManager.getMaxPendingPeers(torrentStatus);
		return needEnoughPeers && notExceedMaxPendingPeers;
	}

	public int getMaxPeers() {
		return peerManager.getMaxPeers(torrentStatus);
	}

	public int peersWanted() {
		return peerManager.getAnnounceWantAmount(torrentStatus, peers.size());
	}

	public void pollRates() {
		for (int i = 0; i < peers.size(); i++) {
			peers.get(i).pollRates();
		}
	}

	public Files getFiles() {
		return files;
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
		if (torrentStatus == STATE_DOWNLOAD_METADATA)
			return 0;
		int seeds = 0;
		if (files == null || peers == null)
			return 0;
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p == null)
				continue;
			if (p.getClient().getBitfield().hasPieceCount() == files.getPieceCount())
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

	public ArrayList<Peer> getPeers() {
		return peers;
	}

	/**
	 * The amount of bytes downloaded
	 * 
	 * @return
	 */
	public long getDownloadedBytes() {
		return downloadedBytes;
	}
	
	/**
	 * The amount of bytes we have uploaded this session
	 * 
	 * @return
	 */
	public long getUploadedBytes() {
		return uploadedBytes;
	}

	/**
	 * Gets all lecheers which: has atleast 1 piece and has us unchoked
	 * 
	 * @return
	 */
	public ArrayList<Peer> getDownloadablePeers() {
		ArrayList<Peer> leechers = new ArrayList<Peer>();
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (p == null)
				continue;
			if (torrentStatus == STATE_DOWNLOAD_METADATA) {
				if (p.getClient().hasExtentionID(UTMetadata.NAME))
					leechers.add(p);
			} else {
				if (p.getClient().getBitfield().hasPieceCount() > 0 && !p.getMyClient().isChoked() && p.getFreeWorkTime() > 0)
					leechers.add(p);
			}
		}
		return leechers;
	}
	
	/**
	 * The regulator which is managing this download
	 * @return The current assigned regulator
	 */
	public IDownloadRegulator getDownloadRegulator() {
		return downloadRegulator;
	}

	@Override
	public boolean equals(Object object) {
		if(object instanceof Torrent) {
			Torrent torrent = (Torrent)object;
			return SHA1.match(btihHash, torrent.btihHash);
		} else {
			return false;
		}
	}

}
