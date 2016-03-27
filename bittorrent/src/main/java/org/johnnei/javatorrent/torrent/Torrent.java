package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Torrent {

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
	 * The amount of downloaded bytes
	 */
	private long downloadedBytes;
	/**
	 * The amount of uploaded bytes
	 */
	private long uploadedBytes;

	/**
	 * The torrent client which created this Torrent object.
	 */
	private TorrentClient torrentClient;

	/**
	 * Creates a new Torrent.
	 *
	 * @param builder The builder with the components for the torrent.
	 */
	public Torrent(Builder builder) {
		displayName = Argument.requireNonNull(builder.displayName, "Torrent name is required");
		torrentClient = builder.torrentClient;
		btihHash = builder.hash;
		downloadedBytes = 0L;
		peers = new LinkedList<>();
		pieceSelector = new FullPieceSelect(this);
	}

	private boolean hasPeer(Peer peer) {
		synchronized (this) {
			return peers.contains(peer);
		}
	}

	/**
	 * Adds a peer to the torrent if not already registered to the torrent. Upon accepting the socket will be marked as passed handshake and the currently
	 * available message will be shared via either {@link MessageBitfield} or one or more {@link MessageHave}.
	 * @param peer The peer to add.
	 * @throws IOException
	 */
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

	/**
	 * Removes a peer from the torrent. This will also clean up the peer state which affects the progress state of the torrent (ex. pending block requests).
	 * @param peer the peer to remove.
	 */
	public void removePeer(Peer peer) {
		Argument.requireNonNull(peer, "Peer can not be null");

		synchronized (this) {
			if (!peers.remove(peer)) {
				return;
			}
		}

		peer.discardAllBlockRequests();
	}

	private void sendHaveMessages(Peer peer) throws IOException {
		if (isDownloadingMetadata()) {
			return;
		}

		if (files.countCompletedPieces() == 0) {
			return;
		}

		final int bitfieldOverhead = 1;
		final int bitfieldPacketSize = files.getBitfieldBytes().length + bitfieldOverhead;

		final int haveOverheadPerPiece = 5;
		final int havePacketsSize = files.countCompletedPieces() * haveOverheadPerPiece;

		if (bitfieldPacketSize < havePacketsSize) {
			peer.getBitTorrentSocket().enqueueMessage(new MessageBitfield(files.getBitfieldBytes()));
		} else {
			for (int pieceIndex = 0; pieceIndex < files.getPieceCount(); pieceIndex++) {
				if (!files.hasPiece(pieceIndex)) {
					continue;
				}

				peer.getBitTorrentSocket().enqueueMessage(new MessageHave(pieceIndex));
			}
		}
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Gets the 20 byte BTIH hash of the torrent.
	 * @return The 20 byte BTIH hash.
	 */
	public byte[] getHashArray() {
		return btihHash;
	}

	/**
	 * Gets the {@link #getHashArray()} formatted as hexadecimal.
	 * @return The BTIH hash in hexadecimal.
	 *
	 * @see #getHashArray()
	 */
	public String getHash() {
		return StringUtils.byteArrayToString(btihHash);
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

	/**
	 * Tells the torrent to save a block of data
	 *
	 * @param index The piece index
	 * @param offset The offset within the piece
	 * @param data The bytes to be stored
	 */
	public void onReceivedBlock(int index, int offset, byte[] data) {
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

		if (!piece.isDone()) {
			return;
		}

		addDiskJob(new DiskJobCheckHash(piece, this::onCheckPieceHashComplete));
	}

	private void onCheckPieceHashComplete(DiskJobCheckHash checkJob) {
		if (isDownloadingMetadata()) {
			return;
		}

		Piece piece = checkJob.getPiece();
		if (!checkJob.isMatchingHash()) {
			piece.onHashMismatch();
			return;
		}

		files.setHavingPiece(checkJob.getPiece().getIndex());
		broadcastMessage(new MessageHave(checkJob.getPiece().getIndex()));
		downloadedBytes += piece.getSize();
	}

	/**
	 * Adds a task to the IOManager of this torrent
	 *
	 * @param task The task to add
	 */
	public void addDiskJob(IDiskJob task) {
		torrentClient.addDiskJob(task);
	}

	private void broadcastMessage(IMessage m) {
		synchronized (this) {
			peers.stream().forEach(p -> p.getBitTorrentSocket().enqueueMessage(m));
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
	public void setFileSet(AbstractFileSet files) {
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
	 * the files in the torrent or something else if a module changed it with {@link #setFileSet(AbstractFileSet)}.
	 *
	 * @return The set of files being downloaded.
	 * @see #setFileSet(AbstractFileSet)
	 */
	public AbstractFileSet getFileSet() {
		return files;
	}

	/**
	 * Sums the download rates of all peers.
	 * @return The sum of all download rates
	 *
	 * @see #pollRates()
	 */
	public int getDownloadRate() {
		synchronized (this) {
			return peers.stream().mapToInt(p -> p.getBitTorrentSocket().getDownloadRate()).sum();
		}
	}

	/**
	 * Sums the upload rates of all peers.
	 * @return The sum of all upload rates
	 *
	 * @see #pollRates()
	 */
	public int getUploadRate() {
		synchronized (this) {
			return peers.stream().mapToInt(p -> p.getBitTorrentSocket().getUploadRate()).sum();
		}
	}

	/**
	 * Counts the amount of peers which have all pieces.
	 * @return The amount of connected seeders.
	 */
	public int getSeedCount() {
		if (isDownloadingMetadata()) {
			return 0;
		}

		synchronized (this) {
			return (int) peers.stream().filter(p -> p.countHavePieces() == files.getPieceCount()).count();
		}
	}

	/**
	 * Counts the amount of peers which don't have all pieces yet.
	 * @return The amount of connected leechers.
	 */
	public int getLeecherCount() {
		synchronized (this) {
			return (int) (peers.stream().count() - getSeedCount());
		}
	}

	/**
	 * Creates a copy of the list of connected peers.
	 * @return The list of connected peers.
	 */
	public List<Peer> getPeers() {
		return new ArrayList<>(peers);
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
