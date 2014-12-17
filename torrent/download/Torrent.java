package torrent.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.johnnei.utils.ConsoleLogger;
import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.TorrentException;
import torrent.TorrentManager;
import torrent.download.algos.BurstPeerManager;
import torrent.download.algos.FullPieceSelect;
import torrent.download.algos.IDownloadPhase;
import torrent.download.algos.IDownloadRegulator;
import torrent.download.algos.IPeerManager;
import torrent.download.algos.PhasePreMetadata;
import torrent.download.files.Piece;
import torrent.download.files.disk.DiskJob;
import torrent.download.files.disk.DiskJobStoreBlock;
import torrent.download.files.disk.IOManager;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
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

public class Torrent implements Runnable {

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
	private AFiles files;
	
	/**
	 * Contains the information about the metadata backing this torrent.
	 */
	private MetadataFile metadata;
	
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
	
	private Logger log;
	
	/**
	 * The thread on which the torrent is being processed
	 */
	private Thread thread;

	public static final byte STATE_DOWNLOAD_METADATA = 0;
	public static final byte STATE_DOWNLOAD_DATA = 1;
	public static final byte STATE_UPLOAD = 2;

	public Torrent(TorrentManager manager, TrackerManager trackerManager, byte[] btihHash, String displayName) {
		log = ConsoleLogger.createLogger(String.format("Torrent %s", StringUtil.byteArrayToString(btihHash)), Level.INFO);
		this.displayName = displayName;
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
		phase = new PhasePreMetadata(trackerManager, this);
		
		thread = new Thread(this, displayName);
	}

	private boolean hasPeer(Peer peer) {
		synchronized (this) {
			return peers.stream().filter(p -> p.equals(peer)).findAny().isPresent();
		}
	}

	public void addPeer(Peer peer) {
		if (hasPeer(peer)) {
			peer.getBitTorrentSocket().close();
			log.fine("Filtered duplicate Peer: " + peer);
			return;
		}
		synchronized (this) {
			peers.add(peer);
		}
	}
	
	/**
	 * Registers the torrent and starts downloading it
	 */
	public void start() {
		manager.addTorrent(this);
		thread.start();
	}

	public void run() {
		while(phase != null) {
			torrentStatus = phase.getId();
			log.info(String.format("Torrent phase completed. New phase: %s", phase.getClass().getSimpleName()));
			
			for (int i = 0; i < peers.size(); i++) {
				Peer peer = peers.get(i);
				
				if (peer == null) {
					continue;
				}
				
				peer.onTorrentPhaseChange();
			}
			
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
		log.info("Torrent has finished");
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
			if (p.getBitTorrentSocket().closed()) {
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
		List<Piece> neededPieces = files.getNeededPieces().collect(Collectors.toList());
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			boolean hasNoPieces = true;
			for (int j = 0; j < neededPieces.size(); j++) {
				if (p.hasPiece(neededPieces.get(j).getIndex())) {
					hasNoPieces = false;
					if (!p.isInterested(PeerDirection.Upload)) {
						p.getBitTorrentSocket().queueMessage(new MessageInterested());
						p.setInterested(PeerDirection.Upload, true);
					}
					break;
				}
			}
			if (hasNoPieces && p.isInterested(PeerDirection.Upload)) {
				p.getBitTorrentSocket().queueMessage(new MessageUninterested());
				p.setInterested(PeerDirection.Upload, false);
			}
			if (p.isInterested(PeerDirection.Download) && p.isChoked(PeerDirection.Upload)) {
				p.getBitTorrentSocket().queueMessage(new MessageUnchoke());
				p.setChoked(PeerDirection.Upload, false);
			} else if (!p.isInterested(PeerDirection.Download) && !p.isChoked(PeerDirection.Upload)) {
				p.getBitTorrentSocket().queueMessage(new MessageChoke());
				p.setChoked(PeerDirection.Upload, true);
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

	public boolean keepDownloading() {
		return keepDownloading;
	}

	public String getStatus() {
		return status;
	}

	public byte getDownloadStatus() {
		return torrentStatus;
	}

	public double getProgress() {
		if (files == null || files.getPieceCount() == 0) {
			return 0D;
		}
		return 100D * (files.countCompletedPieces() / (double) files.getPieceCount());
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
		downloadedBytes += files.getPiece(pieceIndex).getSize();
		files.havePiece(pieceIndex);
		for (int i = 0; i < peers.size(); i++) {
			Peer p = peers.get(i);
			if (!p.getBitTorrentSocket().closed() && p.getBitTorrentSocket().getPassedHandshake()) {
				p.getBitTorrentSocket().queueMessage(have);
			}
		}
	}

	public void broadcastMessage(IMessage m) {
		for (int i = 0; i < peers.size(); i++) {
			Peer peer = peers.get(i);
			if (!peer.getBitTorrentSocket().closed() && peer.getBitTorrentSocket().getPassedHandshake())
				peer.getBitTorrentSocket().queueMessage(m);
		}
	}

	/**
	 * Calculates the current progress based on all available files on the HDD
	 */
	public void checkProgress() {
		log.info("Checking progress...");
		files.pieces.stream().
			filter(p -> {
				try {
					return p.checkHash();
				} catch (IOException | TorrentException e) {
					log.warning(String.format("Failed hash check for piece %d: %s", p.getIndex(), e.getMessage()));
					return false; 
				}
			}).
			forEach(p -> files.havePiece(p.getIndex())
		);
		log.info("Checking progress done");
	}
	
	/**
	 * Adds the amount of bytes to the uploaded count
	 * 
	 * @param l The amount of bytes to add
	 */
	public void addUploadedBytes(long l) {
		uploadedBytes += l;
	}
	
	public void setFiles(AFiles files) {
		this.files = files;
	}
	
	/**
	 * Sets the associated metadata file of the torrent
	 * @param metadata the metadata which is backing this torrent
	 */
	public void setMetadata(MetadataFile metadata) {
		this.metadata = metadata;
	}
	
	public MetadataFile getMetadata() throws IllegalStateException {
		if (getDownloadStatus() == STATE_DOWNLOAD_METADATA) {
			throw new IllegalStateException("Torrent is still downloading the associated metadata file.");
		}
		
		return metadata;
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
			peers.get(i).getBitTorrentSocket().pollRates();
		}
	}
	
	/**
	 * Gets the files which are being downloaded within this torrent
	 * @return
	 */
	public AFiles getFiles() {
		return files;
	}
	
	public int getDownloadRate() {
		int dlRate = 0;
		for (int i = 0; i < peers.size(); i++) {
			dlRate += peers.get(i).getBitTorrentSocket().getDownloadRate();
		}
		return dlRate;
	}

	public int getUploadRate() {
		int ulRate = 0;
		for (int i = 0; i < peers.size(); i++) {
			ulRate += peers.get(i).getBitTorrentSocket().getUploadRate();
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
			if (p.countHavePieces() == files.getPieceCount())
				++seeds;
		}

		return seeds;
	}

	public int getLeecherCount() {
		synchronized (this) {
			return (int) (peers.stream().filter(p -> p.getBitTorrentSocket().getPassedHandshake()).count() - getSeedCount());
		}
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
				if (p.getExtensions().hasExtension(UTMetadata.NAME))
					leechers.add(p);
			} else {
				if (p.countHavePieces() > 0 && !p.isChoked(PeerDirection.Download) && p.getFreeWorkTime() > 0)
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
	
	public Logger getLogger() {
		return log;
	}

	public void setMetadataSize(int i) {
		if (phase.getId() != STATE_DOWNLOAD_METADATA) {
			return;
		}
		
		if (phase instanceof PhasePreMetadata) {
			((PhasePreMetadata)phase).setMetadataSize(i);
		}
	}

	public void setDownloadRegulator(IDownloadRegulator downloadRegulator) {
		this.downloadRegulator = downloadRegulator;
	}

}
