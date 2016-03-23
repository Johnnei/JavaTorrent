package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.disk.DiskJobCheckHash;
import org.johnnei.javatorrent.disk.DiskJobWriteBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.disk.IOManager;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.peermanager.IPeerManager;
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.MathUtils;
import org.johnnei.javatorrent.utils.StringUtils;
import org.johnnei.javatorrent.utils.ThreadUtils;

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

	/**
	 * Contains all data of the actual torrent
	 */
	private AbstractFileSet files;

	/**
	 * Contains the information about the metadata backing this torrent.
	 */
	private MetadataFileSet metadata;

	/**
	 * Regulates the selection of pieces and the peers to download the pieces
	 */
	private IPieceSelector pieceSelector;
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
	 * IOManager to manage the transaction between the hdd and the programs so none of the actual network thread need to get block for that
	 */
	private IOManager ioManager;
	/**
	 * The phase in which the torrent currently is
	 */
	private IDownloadPhase phase;

	/**
	 * The strategy used to handle the choking of peers.
	 */
	private IChokingStrategy chokingStrategy;

	/**
	 * The torrent client which created this Torrent object.
	 */
	private TorrentClient torrentClient;

	/**
	 * The thread on which the torrent is being processed
	 */
	private Thread thread;

	/**
	 * Creates a new Torrent.
	 *
	 * @param builder The builder with the components for the torrent.
	 */
	public Torrent(Builder builder) {
		displayName = Argument.requireNonNull(builder.displayName, "Torrent name is required");
		torrentClient = builder.torrentClient;
		btihHash = builder.hash;
		peerManager = builder.peerManager;
		phase = builder.phase;
		chokingStrategy = builder.chokingStrategy;
		downloadedBytes = 0L;
		peers = new LinkedList<>();
		ioManager = new IOManager();
		pieceSelector = new FullPieceSelect(this);
		thread = new Thread(this, displayName);
	}

	private boolean hasPeer(Peer peer) {
		synchronized (this) {
			return peers.contains(peer);
		}
	}

	public void addPeer(Peer peer) throws IOException {
		Argument.requireNonNull(peer, "Peer can not be null");

		if (hasPeer(peer)) {
			peer.getBitTorrentSocket().close();
			LOGGER.trace("Filtered duplicate Peer: {}", peer);
			return;
		}

		peer.getBitTorrentSocket().setPassedHandshake();
		sendHaveMessages(peer);

		synchronized (this) {
			peers.add(peer);
		}
	}

	private void sendHaveMessages(Peer peer) throws IOException {
		if (peer.getTorrent().isDownloadingMetadata()) {
			return;
		}

		Torrent torrent = peer.getTorrent();
		AbstractFileSet files = torrent.getFiles();

		if (files.countCompletedPieces() == 0) {
			return;
		}

		if (MathUtils.ceilDivision(torrent.getFiles().getPieceCount(), 8) + 1 < 5 * files.countCompletedPieces()) {
			peer.getBitTorrentSocket().enqueueMessage(new MessageBitfield(files.getBitfieldBytes()));
		} else {
			for (int pieceIndex = 0; pieceIndex < torrent.getFiles().getPieceCount(); pieceIndex++) {
				if (!torrent.getFiles().hasPiece(pieceIndex)) {
					continue;
				}

				peer.getBitTorrentSocket().enqueueMessage(new MessageHave(pieceIndex));
			}
		}
	}

	/**
	 * Registers the torrent and starts downloading it
	 */
	public void start() {
		thread.start();
	}

	@Override
	public void run() {
		while (phase != null) {
			LOGGER.info(String.format("Torrent phase completed. New phase: %s", phase.getClass().getSimpleName()));
			synchronized (this) {
				peers.forEach(Peer::onTorrentPhaseChange);
			}
			phase.onPhaseEnter();
			while (!phase.isDone()) {
				processPeers();
				phase.process();
				ioManager.processTask();
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
					forEach(Peer::cancelAllPieces);
			peers.removeIf(p -> p == null || p.getBitTorrentSocket().closed());
			peers.forEach(Peer::checkDisconnect);

		}
	}

	/**
	 * Updates the interested states
	 */
	private void updatePeers() {
		if (files == null) {
			return;
		}
		synchronized (this) {
			for (Peer p : peers) {
				chokingStrategy.updateChoking(p);
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
		return StringUtils.byteArrayToString(btihHash);
	}

	public IDownloadPhase getDownloadPhase() {
		return phase;
	}

	/**
	 * If the Torrent should be downloading the metadata information.
	 * Depending on the installed modules the torrent might be stuck at this point.
	 * BEP 10 and UT_METADATA extension must be available to download metadata.
	 *
	 * @return <code>true</code> when the torrent is currently downloading the metadata, otherwise <code>false</code>
	 */
	public boolean isDownloadingMetadata() {
		if (metadata == null) {
			return true;
		}

		return !metadata.isDone();
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
		int blockIndex = offset / files.getBlockSize();

		Piece piece = files.getPiece(index);
		if (piece.getBlockSize(blockIndex) != data.length) {
			piece.setBlockStatus(blockIndex, BlockStatus.Needed);
		} else {
			addDiskJob(new DiskJobWriteBlock(files.getPiece(index), blockIndex, data, this::onStoreBlockComplete));
		}
	}

	private void onStoreBlockComplete(DiskJobWriteBlock storeBlock) {
		Piece piece = storeBlock.getPiece();
		piece.setBlockStatus(storeBlock.getBlockIndex(), BlockStatus.Stored);

		if (!files.getPiece(piece.getIndex()).isDone()) {
			return;
		}

		addDiskJob(new DiskJobCheckHash(piece, this::onCheckPieceHashComplete));
	}

	private void onCheckPieceHashComplete(DiskJobCheckHash checkJob) {
		if (isDownloadingMetadata()) {
			return;
		}

		Piece piece = checkJob.getPiece();
		if (checkJob.isMatchingHash()) {
			piece.onHashMismatch();
			return;
		}

		files.setHavingPiece(checkJob.getPiece().getIndex());
		broadcastMessage(new MessageHave(checkJob.getPiece().getIndex()));
	}

	/**
	 * Adds a task to the IOManager of this torrent
	 *
	 * @param task The task to add
	 */
	public void addDiskJob(IDiskJob task) {
		ioManager.addTask(task);
	}

	private void broadcastMessage(IMessage m) {
		synchronized (this) {
			peers.stream().
					filter(p -> !p.getBitTorrentSocket().closed() && p.getBitTorrentSocket().getPassedHandshake()).
					forEach(p -> p.getBitTorrentSocket().enqueueMessage(m));
		}
	}

	/**
	 * Calculates the current progress based on all available files on the HDD
	 */
	public void checkProgress() {
		LOGGER.info("Checking progress...");
		files.getNeededPieces()
				.filter(p -> {
					try {
						return p.checkHash();
					} catch (IOException e) {
						LOGGER.warn("Failed hash check for piece {}.", p.getIndex(), e);
						return false;
					}
				}).
				forEach(p -> {
							files.setHavingPiece(p.getIndex());
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

	/**
	 * Sets the current set of files this torrent is downloading.
	 *
	 * @param files The file set.
	 */
	public void setFiles(AbstractFileSet files) {
		this.files = files;
	}

	/**
	 * Sets the associated metadata file of the torrent
	 *
	 * @param metadata the metadata which is backing this torrent
	 */
	public void setMetadata(MetadataFileSet metadata) {
		this.metadata = Argument.requireNonNull(metadata, "Metadata can not be set to null");
	}

	public Optional<MetadataFileSet> getMetadata() {
		return Optional.ofNullable(metadata);
	}

	@Override
	public String toString() {
		return String.format("Torrent[hash=%s]", getHash());
	}

	public int peersWanted() {
		return peerManager.getAnnounceWantAmount(peers.size());
	}

	/**
	 * Polls all peers transfer speeds.
	 */
	public void pollRates() {
		synchronized (this) {
			peers.forEach(p -> p.getBitTorrentSocket().pollRates());
		}
	}

	/**
	 * Gets the files which are being downloaded within this torrent. This could be the metadata of the torrent (.torrent file),
	 * the files in the torrent or something else if a module changed it with {@link #setFiles(AbstractFileSet)}.
	 *
	 * @return The set of files being downloaded.
	 * @see #setFiles(AbstractFileSet)
	 */
	public AbstractFileSet getFiles() {
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
	 * @return The amount of bytes downloaded this session
	 */
	public long getDownloadedBytes() {
		return downloadedBytes;
	}

	/**
	 * The amount of bytes we have uploaded this session
	 *
	 * @return The amount of bytes uploaded this session.
	 */
	public long getUploadedBytes() {
		return uploadedBytes;
	}

	/**
	 * Filters the list of peer to the set of relevant ones for this download phase.
	 *
	 * @return The list of relevant peers.
	 */
	public synchronized Collection<Peer> getRelevantPeers() {
		return phase.getRelevantPeers(peers);
	}

	/**
	 * The regulator which is managing this download
	 *
	 * @return The current assigned regulator
	 */
	public IPieceSelector getPieceSelector() {
		return pieceSelector;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(btihHash);
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

		return Arrays.equals(btihHash, other.btihHash);
	}

	public void setPieceSelector(IPieceSelector downloadRegulator) {
		this.pieceSelector = downloadRegulator;
	}

	public static final class Builder {

		private TorrentClient torrentClient;

		private String displayName;

		private IPeerManager peerManager;

		private IDownloadPhase phase;

		private IChokingStrategy chokingStrategy;

		private byte[] hash;

		public Builder setTorrentClient(TorrentClient torrentClient) {
			this.torrentClient = torrentClient;
			return this;
		}

		public Builder setHash(byte[] hash) {
			this.hash = hash;
			return this;
		}

		public Builder setName(String name) {
			this.displayName = name;
			return this;
		}

		public Builder setPeerManager(IPeerManager peerManager) {
			this.peerManager = peerManager;
			return this;
		}

		public Builder setInitialPhase(IDownloadPhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setChokingStrategy(IChokingStrategy chokingStrategy) {
			this.chokingStrategy = chokingStrategy;
			return this;
		}

		/**
		 * This method check if {@link #setHash(byte[])} has been called as depending on the configured BEPs that requirements
		 * before something is 'downloadable' changes.
		 *
		 * @return <code>true</code> if the hash has been set.
		 */
		public boolean canDownload() {
			return hash != null;
		}

		public Torrent build() {
			return new Torrent(this);
		}

	}

}
