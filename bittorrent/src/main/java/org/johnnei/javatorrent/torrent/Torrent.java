package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.disk.DiskJobCheckHash;
import org.johnnei.javatorrent.disk.DiskJobWriteBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.torrent.algos.requests.IRequestLimiter;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;

public class Torrent {

	private static final Logger LOGGER = LoggerFactory.getLogger(Torrent.class);

	private static final Collection<BlockStatus> BLOCK_WRITABLE = EnumSet.of(BlockStatus.Needed, BlockStatus.Requested);

	/**
	 * The display name of this torrent
	 */
	private String displayName;

	/**
	 * All connected peers
	 */
	private List<Peer> peers;

	/**
	 * Contains all data of the actual torrent
	 */
	private TorrentFileSet fileSet;

	/**
	 * Contains the information about the metadata backing this torrent.
	 */
	private Metadata metadata;

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
		this.metadata = Argument.requireNonNull(builder.metadata, "Torrent without minimal metadata information is not downloadable.");
		if (builder.displayName == null) {
			displayName = builder.metadata.getName();
		} else {
			displayName = builder.displayName;
		}
		torrentClient = builder.torrentClient;
		downloadedBytes = 0L;
		peers = new LinkedList<>();
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

		for (IModule module : torrentClient.getModules()) {
			module.onPostHandshake(peer);
		}
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
			peers.remove(peer);
		}
	}

	private void sendHaveMessages(Peer peer) throws IOException {
		if (isDownloadingMetadata()) {
			return;
		}

		if (fileSet.countCompletedPieces() == 0) {
			return;
		}

		final int bitfieldOverhead = 1;
		final int bitfieldPacketSize = fileSet.getBitfieldBytes().length + bitfieldOverhead;

		final int haveOverheadPerPiece = 5;
		final int havePacketsSize = fileSet.countCompletedPieces() * haveOverheadPerPiece;

		if (bitfieldPacketSize < havePacketsSize) {
			peer.getBitTorrentSocket().enqueueMessage(new MessageBitfield(fileSet.getBitfieldBytes()));
		} else {
			for (int pieceIndex = 0; pieceIndex < fileSet.getPieceCount(); pieceIndex++) {
				if (!fileSet.hasPiece(pieceIndex)) {
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
	 * If the Torrent should be downloading the metadata information.
	 * Depending on the installed modules the torrent might be stuck at this point.
	 * BEP 10 and UT_METADATA extension must be available to download metadata.
	 *
	 * @return <code>true</code> when the torrent is currently downloading the metadata, otherwise <code>false</code>
	 */
	public boolean isDownloadingMetadata() {
		return fileSet == null;
	}

	/**
	 * Tells the torrent to save a block of data
	 *
	 * @param fileSet The fileset for which the block of data has been received.
	 * @param index The piece index
	 * @param offset The offset within the piece
	 * @param data The bytes to be stored
	 */
	public void onReceivedBlock(AbstractFileSet fileSet, int index, int offset, byte[] data) {
		int blockIndex = offset / fileSet.getBlockSize();

		Piece piece = fileSet.getPiece(index);
		if (BLOCK_WRITABLE.contains(piece.getBlockStatus(blockIndex))) {
			if (piece.getBlockSize(blockIndex) != data.length) {
				LOGGER.debug("Received incorrect sized block for piece {}, offset {}", index, offset);
				piece.setBlockStatus(blockIndex, BlockStatus.Needed);
			} else {
				addDiskJob(new DiskJobWriteBlock(piece, blockIndex, data, this::onStoreBlockComplete));
			}
		}
	}

	private void onStoreBlockComplete(DiskJobWriteBlock storeBlock) {
		Piece piece = storeBlock.getPiece();
		piece.setBlockStatus(storeBlock.getBlockIndex(), BlockStatus.Stored);

		if (piece.countBlocksWithStatus(BlockStatus.Stored) != piece.getBlockCount()) {
			return;
		}

		addDiskJob(new DiskJobCheckHash(piece, this::onCheckPieceHashComplete));
	}

	private void onCheckPieceHashComplete(DiskJobCheckHash checkJob) {
		Piece piece = checkJob.getPiece();
		if (!checkJob.isMatchingHash()) {
			LOGGER.debug("Piece hash mismatched");
			piece.onHashMismatch();
			return;
		}

		piece.getFileSet().setHavingPiece(piece.getIndex());
		if (piece.getFileSet().equals(fileSet)) {
			broadcastMessage(new MessageHave(piece.getIndex()));
			downloadedBytes += piece.getSize();
		}

		LOGGER.debug("Completed piece {}", piece.getIndex());
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
	 * Calculates the current progress based on all available fileSet on the HDD
	 */
	public void checkProgress() {
		LOGGER.info("Checking progress...");
		fileSet.getNeededPieces()
				.filter(p -> {
					try {
						return p.checkHash();
					} catch (IOException e) {
						LOGGER.warn("Failed hash check for piece {}.", p.getIndex(), e);
						return false;
					}
				}).
				forEach(p -> {
						fileSet.setHavingPiece(p.getIndex());
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
	 * Sets the current set of fileSet this torrent is downloading.
	 *
	 * @param files The file set.
	 */
	public void setFileSet(TorrentFileSet files) {
		this.fileSet = files;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return String.format("Torrent[hash=%s]", metadata.getHashString());
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
	 * Gets the fileSet which are being downloaded within this torrent. This could be the metadata of the torrent (.torrent file),
	 * the fileSet in the torrent or something else if a module changed it with {@link #setFileSet(TorrentFileSet)}.
	 *
	 * @return The set of fileSet being downloaded.
	 * @see #setFileSet(TorrentFileSet)
	 */
	public TorrentFileSet getFileSet() {
		return fileSet;
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
			return (int) peers.stream().filter(p -> p.countHavePieces() == fileSet.getPieceCount()).count();
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
		synchronized (this) {
			return new ArrayList<>(peers);
		}
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

	@Override
	public int hashCode() {
		return Arrays.hashCode(metadata.getHash());
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

		return metadata.equals(other.metadata);
	}

	public IRequestLimiter getRequestLimiter() {
		return torrentClient.getRequestLimiter();
	}

	/**
	 * A builder to create new instances of {@link Torrent}
	 */
	public static final class Builder {

		private TorrentClient torrentClient;

		private File downloadFolder;

		private Metadata metadata;

		private String displayName;

		/**
		 * Sets the torrent client on which this torrent will be registered.
		 * @param torrentClient The client.
		 * @return The adjusted builder.
		 */
		public Builder setTorrentClient(TorrentClient torrentClient) {
			this.torrentClient = torrentClient;
			return this;
		}

		/**
		 * Sets the display name for this torrent.
		 * @param name The name to set.
		 * @return The adjusted builder.
		 */
		public Builder setName(String name) {
			this.displayName = name;
			return this;
		}

		public Builder setMetadata(Metadata metadata) {
			this.metadata = metadata;
			return this;
		}

		public Builder setDownloadFolder(File downloadFolder) {
			this.downloadFolder = downloadFolder;
			return this;
		}

		/**
		 * @return <code>true</code> if the hash of the metadata is available.
		 */
		public boolean canDownload() {
			return metadata != null;
		}

		/**
		 * Creates a torrent without metadata information (the .torrent file is not present).
		 * @return The newly created torrent.
		 */
		public Torrent build() {
			Argument.requireNonNull(metadata, "Torrent without minimal metadata information is not downloadable.");
			Torrent torrent = new Torrent(this);
			if (downloadFolder == null) {
				downloadFolder = new File(metadata.getName());
			}

			if (!metadata.getFileEntries().isEmpty()) {
				TorrentFileSet fileSet = new TorrentFileSet(metadata, downloadFolder);
				torrent.setFileSet(fileSet);
			}

			return torrent;
		}
	}

}
