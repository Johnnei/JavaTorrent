package org.johnnei.javatorrent.torrent.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.algos.FullPieceSelect;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadRegulator;
import org.johnnei.javatorrent.torrent.download.algos.IPeerManager;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.files.disk.DiskJob;
import org.johnnei.javatorrent.torrent.download.files.disk.DiskJobStoreBlock;
import org.johnnei.javatorrent.torrent.download.files.disk.IOManager;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.torrent.util.StringUtil;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.johnnei.javatorrent.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Torrent implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Torrent.class);

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
	private List<Peer> peers;

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
	private Optional<MetadataFile> metadata;

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
	private AtomicInteger torrentHaltingOperations;

	/**
	 * IOManager to manage the transaction between the hdd and the programs so none of the actual network thread need to get block for that
	 */
	private IOManager ioManager;
	/**
	 * The phase in which the torrent currently is
	 */
	private IDownloadPhase phase;

	/**
	 * The torrent client which created this Torrent object.
	 */
	private TorrentClient torrentClient;

	/**
	 * The thread on which the torrent is being processed
	 */
	private Thread thread;

	public Torrent(TorrentClient torrentClient, byte[] btihHash, String displayName) {
		this.displayName = displayName;
		this.torrentClient = torrentClient;
		this.btihHash = btihHash;
		torrentHaltingOperations = new AtomicInteger();
		downloadedBytes = 0L;
		peers = new LinkedList<Peer>();
		keepDownloading = true;
		status = "Parsing Magnet Link";
		ioManager = new IOManager();
		downloadRegulator = new FullPieceSelect(this);
		peerManager = torrentClient.getPeerManager();
		phase = torrentClient.getPhaseRegulator().createInitialPhase(torrentClient, this);

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
			LOGGER.trace("Filtered duplicate Peer: " + peer);
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
		torrentClient.getTorrentManager().addTorrent(this);
		thread.start();
	}

	@Override
	public void run() {
		while(phase != null) {
			LOGGER.info(String.format("Torrent phase completed. New phase: %s", phase.getClass().getSimpleName()));
			synchronized (this) {
				peers.forEach(p -> p.onTorrentPhaseChange());
			}
			phase.onPhaseEnter();
			while (!phase.isDone() || torrentHaltingOperations.get() > 0) {
				processPeers();
				phase.process();
				ioManager.processTask(this);
				ThreadUtils.sleep(25);
			}
			phase.onPhaseExit();
			phase = torrentClient.getPhaseRegulator().createNextPhase(phase, torrentClient, this).orElse(null);
		}
		LOGGER.info("Torrent has finished");
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
		synchronized (this) {
			peers.stream().
				filter(p -> p.getBitTorrentSocket().closed()).
				forEach(p -> p.cancelAllPieces());
			peers.removeIf(p -> p == null || p.getBitTorrentSocket().closed());
			peers.forEach(p -> p.checkDisconnect());

		}
	}

	/**
	 * Updates the interested states
	 */
	private void updatePeers() {
		if (files == null) {
			return;
		}
		List<Piece> neededPieces = files.getNeededPieces().collect(Collectors.toList());
		synchronized (this) {
			for (Peer p : peers) {
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

	public IDownloadPhase getDownloadPhase() {
		return phase;
	}

	/**
	 * If the Torrent should be downloading the metadata information.
	 * Depending on the installed modules the torrent might be stuck at this point.
	 * BEP 10 and UT_METADATA extension must be available to download metadata.
	 * @return
	 */
	public boolean isDownloadingMetadata() {
		if (!metadata.isPresent()) {
			return true;
		}

		if (!metadata.get().isDone()) {
			return true;
		}

		return false;
	}

	public double getProgress() {
		if (isDownloadingMetadata() || files == null || files.getPieceCount() == 0) {
			return 0D;
		}
		return (files.countCompletedPieces() * 100d) / files.getPieceCount();
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

	public void addToHaltingOperations(int newTasks) {
		for (int i = 0; i < newTasks; i++) {
			torrentHaltingOperations.incrementAndGet();
		}
	}


	/**
	 * Called by IOManager to notify the torrent that we processed the collectingPiece
	 */
	public void finishHaltingOperations(int completedTasks) {
		for (int i = 0; i < completedTasks; i++) {
			torrentHaltingOperations.decrementAndGet();
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

	public void broadcastMessage(IMessage m) {
		synchronized (this) {
			peers.stream().
				filter(p -> !p.getBitTorrentSocket().closed() && p.getBitTorrentSocket().getPassedHandshake()).
				forEach(p -> p.getBitTorrentSocket().queueMessage(m));
		}
	}

	/**
	 * Calculates the current progress based on all available files on the HDD
	 */
	public void checkProgress() {
		LOGGER.info("Checking progress...");
		files.pieces.stream().
			filter(p -> {
				try {
					return p.checkHash();
				} catch (IOException | TorrentException e) {
					LOGGER.warn(String.format("Failed hash check for piece %d: %s", p.getIndex(), e.getMessage()));
					return false;
				}
			}).
			forEach(p -> {
				files.havePiece(p.getIndex());
				broadcastMessage(new MessageHave(p.getIndex()));
			}
		);
		LOGGER.info("Checking progress done");
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
		this.metadata = Optional.of(metadata);
	}

	public Optional<MetadataFile> getMetadata() {
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
		boolean notExceedMaxPendingPeers = peers.size() < peerManager.getMaxPendingPeers();
		return needEnoughPeers && notExceedMaxPendingPeers;
	}

	public int getMaxPeers() {
		return peerManager.getMaxPeers();
	}

	public int peersWanted() {
		return peerManager.getAnnounceWantAmount(peers.size());
	}

	public void pollRates() {
		synchronized (this) {
			peers.forEach(p -> p.getBitTorrentSocket().pollRates());
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
		synchronized (this) {
			return peers.stream().mapToInt(p -> p.getBitTorrentSocket().getDownloadRate()).sum();
		}
	}

	public int getUploadRate() {
		synchronized (this) {
			return peers.stream().mapToInt(p -> p.getBitTorrentSocket().getUploadRate()).sum();
		}
	}

	public int getSeedCount() {
		if (isDownloadingMetadata()) {
			return 0;
		}

		synchronized (this) {
			return (int) peers.stream().filter(p -> p.countHavePieces() == files.getPieceCount()).count();
		}
	}

	public int getLeecherCount() {
		synchronized (this) {
			return (int) (peers.stream().filter(p -> p.getBitTorrentSocket().getPassedHandshake()).count() - getSeedCount());
		}
	}

	public List<Peer> getPeers() {
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
	 * Gets all leechers which: has atleast 1 piece and has us unchoked
	 *
	 * @return
	 */
	@Deprecated
	public ArrayList<Peer> getDownloadablePeers() {
		ArrayList<Peer> leechers = new ArrayList<Peer>();
		synchronized (this) {
			for (Peer peer : peers) {
				if (peer.countHavePieces() == 0) {
					continue;
				}

				if (!peer.isChoked(PeerDirection.Download) && peer.getFreeWorkTime() > 0) {
					leechers.add(peer);
				}
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(btihHash);
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
		if (!(obj instanceof Torrent)) {
			return false;
		}
		Torrent other = (Torrent) obj;
		if (!Arrays.equals(btihHash, other.btihHash)) {
			return false;
		}
		return true;
	}

	public void setDownloadRegulator(IDownloadRegulator downloadRegulator) {
		this.downloadRegulator = downloadRegulator;
	}

}
