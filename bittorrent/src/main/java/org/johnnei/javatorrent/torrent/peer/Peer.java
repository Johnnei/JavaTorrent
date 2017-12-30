package org.johnnei.javatorrent.torrent.peer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.internal.torrent.peer.Bitfield;
import org.johnnei.javatorrent.internal.torrent.peer.Client;
import org.johnnei.javatorrent.internal.torrent.peer.Job;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.StringUtils;

public class Peer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Peer.class);
	private static final String LOG_OUTSTANDING_BLOCK_REQUESTS = "Outstanding block requests [{}]";

	/**
	 * The torrent on which this peer is participating.
	 */
	private final Torrent torrent;

	/**
	 * {@link #id} as string.
	 */
	private final String idString;

	/**
	 * The peer id as reported by the peer.
	 */
	private final byte[] id;

	/**
	 * Client information about the connected peer
	 * This will contain the requests the endpoint made of us. {@link PeerDirection#Upload}
	 */
	private final Client peerClient;

	/**
	 * Client information about me retrieved from the connected peer<br>
	 * This will contain the requests we made to the endpoint {@link PeerDirection#Download}
	 */
	private final Client myClient;

	private String clientName;

	/**
	 * The count of messages which are still being processed by the IOManager
	 */
	private int pendingMessages;

	/**
	 * The amount of errors the client made<br>
	 * If the amount reaches 5 the client will be disconnected on the next
	 * peerCheck
	 */
	private int strikes;

	/**
	 * The pieces this peer has
	 */
	private Bitfield haveState;

	/**
	 * The extensions which are supported by this peer
	 */
	private final byte[] extensionBytes;

	/**
	 * The absolute maximum amount of requests which the peer can support<br>
	 * Default value will be {@link Integer#MAX_VALUE}
	 */
	private int absoluteRequestLimit;

	/**
	 * The amount of requests we think this peer can handle at most at the same time.
	 */
	private int requestLimit;

	/**
	 * The bittorrent client which handles this peer's socket information and input/outputstreams
	 */
	private final BitTorrentSocket socket;

	/**
	 * A map of the extra data stored by {@link IModule} which are peer specific
	 */
	private Map<Class<?>, Object> extensions;

	private Peer(Builder builder) {
		this.torrent = Argument.requireNonNull(builder.torrent, "Peer must be assigned to a torrent.");
		this.socket = Argument.requireNonNull(builder.socket, "Peer must have a socket.");
		this.extensionBytes = Argument.requireNonNull(builder.extensionBytes, "Peer extension bytes must be set.");
		this.id = Argument.requireNonNull(builder.id, "Peer ID must be set.");
		this.idString = StringUtils.byteArrayToString(id);

		peerClient = new Client();
		myClient = new Client();
		extensions = new HashMap<>();
		clientName = idString;
		absoluteRequestLimit = Integer.MAX_VALUE;
		if (torrent.getFileSet() != null) {
			haveState = new Bitfield(torrent.getFileSet().getBitfieldBytes().length);
		} else {
			haveState = new Bitfield(0);
		}
		requestLimit = 1;
	}

	/**
	 * Registers the {@link IModule} peer specific information based on the class
	 *
	 * @param infoObject The object to add
	 * @throws IllegalStateException when the class is already registered.
	 * @since 0.5
	 */
	public void addModuleInfo(Object infoObject) {
		Class<?> infoClass = infoObject.getClass();
		if (extensions.containsKey(infoClass)) {
			throw new IllegalStateException(String.format("Module Info already registered: %s", infoClass.getName()));
		}

		extensions.put(infoClass, infoObject);
	}

	/**
	 * Gets the info object registered with {@link #addModuleInfo(Object)}
	 *
	 * @param infoClass The type of information which is stored
	 * @return The module info or {@link Optional#empty()} when not present.
	 * @since 0.5
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getModuleInfo(Class<T> infoClass) {
		if (!extensions.containsKey(infoClass)) {
			return Optional.empty();
		}

		return Optional.of((T) extensions.get(infoClass));
	}

	/**
	 * Checks if the {@link #extensionBytes} has the given bit set for the extension which is part of the extension bytes in the handshake
	 *
	 * @param index The index wihtin the extension bytes.
	 * @param bit The bit to test.
	 * @return returns true if the extension bit is set. Otherwise false
	 */
	public boolean hasExtension(int index, int bit) {
		Argument.requireWithinBounds(index, 0, extensionBytes.length, "Index must be: 0 >= index < 8");

		return (extensionBytes[index] & bit) > 0;
	}

	public void checkDisconnect() {
		if (strikes >= 5) {
			socket.close();
			return;
		}
		Duration inactiveDuration = Duration.between(socket.getLastActivity(), LocalDateTime.now());
		if (inactiveDuration.minusSeconds(30).isNegative()) {
			// Don't send keep alive yet.
			return;
		}

		socket.enqueueMessage(new MessageKeepAlive());
	}

	/**
	 * Adds a download or upload job to the peer. In case of a download request this will also send out a
	 * {@link MessageBlock} for the given block.
	 *
	 * @param piece The requested piece.
	 * @param byteOffset The offset in bytes within the piece.
	 * @param blockLength The amount of bytes requested.
	 * @param type The direction of the request.
	 */
	public void addBlockRequest(Piece piece, int byteOffset, int blockLength, PeerDirection type) {
		Job job = createJob(piece, byteOffset, blockLength, type);
		getClientByDirection(type).addJob(job);

		if (type != PeerDirection.Download) {
			return;
		}

		LOGGER.trace(LOG_OUTSTANDING_BLOCK_REQUESTS, getClientByDirection(PeerDirection.Download).getQueueSize());
		socket.enqueueMessage(piece.getFileSet().getRequestFactory().createRequestFor(this, piece, byteOffset, blockLength));
	}

	/**
	 * Removes the download or upload job from the peer. In case of a download request this will also send out a
	 * {@link org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel} for the given block.
	 *
	 * @param piece The piece to cancel.
	 * @param byteOffset The offset in bytes within the piece.
	 * @param blockLength The amount of bytes requested.
	 * @param type The direction of the request.
	 */
	public void cancelBlockRequest(Piece piece, int byteOffset, int blockLength, PeerDirection type) {
		if (!piece.getFileSet().getRequestFactory().supportsCancellation()) {
			throw new IllegalArgumentException(String.format("The file set of %s doesn't support cancelling piece requests.", piece));
		}

		Job job = createJob(piece, byteOffset, blockLength, type);
		getClientByDirection(type).removeJob(job);

		if (type != PeerDirection.Download) {
			return;
		}

		socket.enqueueMessage(piece.getFileSet().getRequestFactory().createCancelRequestFor(this, piece, byteOffset, blockLength));
		LOGGER.trace(LOG_OUTSTANDING_BLOCK_REQUESTS, getClientByDirection(PeerDirection.Download).getQueueSize());
	}

	/**
	 * Indicates that we've received the requested block from the peer.
	 * @param piece The requested piece.
	 * @param byteOffset The offset in bytes within the piece.
	 */
	public void onReceivedBlock(Piece piece, int byteOffset) {
		int blockLength = piece.getBlockSize(byteOffset / torrent.getFileSet().getBlockSize());
		getClientByDirection(PeerDirection.Download).removeJob(createJob(piece, byteOffset, blockLength, PeerDirection.Download));
		LOGGER.trace(LOG_OUTSTANDING_BLOCK_REQUESTS, getClientByDirection(PeerDirection.Download).getQueueSize());
	}

	private Job createJob(Piece piece, int byteOffset, int blockLength, PeerDirection type) {
		if (type == PeerDirection.Download) {
			return new Job(piece, byteOffset / piece.getFileSet().getBlockSize(), blockLength);
		} else {
			return new Job(piece, byteOffset, blockLength);
		}
	}

	/**
	 * Add a value to the pending Messages count
	 *
	 * @param i The count to add
	 */
	private synchronized void addToPendingMessages(int i) {
		pendingMessages += i;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("Peer[id=%s]", idString);
	}

	/**
	 * Sets the client as reported by the Client in BEP #10.
	 * @param clientName The name of the client.
	 */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	/**
	 * Gets the client name as reported by the BEP #10 extension if supported. Otherwise the name will be an extraction from the Peer ID.
	 * @return The name of the client
	 */
	public String getClientName() {
		return clientName;
	}

	/**
	 * A callback method which gets invoked when the torrent starts a new phase
	 */
	public void onTorrentPhaseChange() {
		if (!torrent.isDownloadingMetadata()) {
			haveState.setSize(torrent.getFileSet().getBitfieldBytes().length);
		}
	}

	/**
	 * Calculates the amount of blocks we can request without overflowing the peer and without slowing us down.
	 * @return The amount of blocks which can still be requested.
	 */
	public int getFreeWorkTime() {
		return Math.max(0, getRequestLimit() - getWorkQueueSize(PeerDirection.Download));
	}

	/**
	 * Gets the amount of pieces the client still needs to send
	 *
	 * @return The amount of blocks which still need to be send/received.
	 */
	public int getWorkQueueSize(PeerDirection direction) {
		return getClientByDirection(direction).getQueueSize();
	}

	/**
	 * Cancels all pieces
	 */
	public void discardAllBlockRequests() {
		synchronized (this) {
			for (Job job : myClient.getJobs()) {
				job.getPiece().setBlockStatus(job.getBlockIndex(), BlockStatus.Needed);
			}
			myClient.clearJobs();
		}
	}

	/**
	 * Registers that this peer has the given piece
	 *
	 * @param pieceIndex the piece to marked as "have"
	 */
	public void setHavingPiece(int pieceIndex) {
		haveState.havePiece(pieceIndex, torrent.isDownloadingMetadata());
	}

	/**
	 * Checks if the peer has the piece with the given index
	 *
	 * @param pieceIndex the piece to check for
	 * @return returns true when the peer has the piece otherwise false
	 */
	public boolean hasPiece(int pieceIndex) {
		return haveState.hasPiece(pieceIndex);
	}

	/**
	 * Gets the time at which the last byte has been read or written to the socket.
	 * @return The most recent activity time
	 */
	public LocalDateTime getLastActivity() {
		return socket.getLastActivity();
	}

	/**
	 * Gets the torrent to which this peer is linked.
	 * @return The torrent.
	 */
	public Torrent getTorrent() {
		return torrent;
	}

	/**
	 * Adds an amount of strikes to the peer
	 *
	 * @param i The amount of strikes to add.
	 */
	public synchronized void addStrike(int i) {
		strikes = Math.max(0, strikes + i);
	}

	/**
	 * Sets the amount of requests this peer can support at most
	 *
	 * @param absoluteRequestLimit The absolute maximum amount of outstanding requests the peer supports.
	 */
	public void setAbsoluteRequestLimit(int absoluteRequestLimit) {
		this.absoluteRequestLimit = absoluteRequestLimit;
		setRequestLimit(Math.min(absoluteRequestLimit, getRequestLimit()));
	}

	/**
	 * Sets the amount of requests we think this peer can handle properly.<br>
	 * This amount will be limited by {@link #absoluteRequestLimit}
	 *
	 * @param requestLimit The new limit.
	 */
	public void setRequestLimit(int requestLimit) {
		if (requestLimit < 0) {
			// This wouldn't make any sense
			return;
		}

		this.requestLimit = Math.min(requestLimit, absoluteRequestLimit);
		LOGGER.trace("Request limit is now [{}]", this.requestLimit);
	}

	/**
	 * Sets if this peer is choked or not.
	 * Choked meaning: We either can or cannot download pieces from this peer (or we don't allow them to do so).
	 * @param direction The side of the connection is being changed
	 * @param choked The choke state
	 */
	public void setChoked(PeerDirection direction, boolean choked) {
		Client client = getClientByDirection(direction);

		if (choked) {
			client.choke();
		} else {
			client.unchoke();
		}

		if (direction == PeerDirection.Upload) {
			if (choked) {
				socket.enqueueMessage(new MessageChoke());
			} else {
				socket.enqueueMessage(new MessageUnchoke());
			}
		}
	}

	/**
	 * Sets if this peer is interested or not.
	 * Interested meaning: We either want or don't want to download pieces from this peer (or they don't want pieces from us).
	 * @param direction The side of the connection is being changed
	 * @param interested The interested state
	 */
	public void setInterested(PeerDirection direction, boolean interested) {
		Client client = getClientByDirection(direction);

		if (interested) {
			client.interested();
		} else {
			client.uninterested();
		}

		if (direction == PeerDirection.Download) {
			if (interested) {
				socket.enqueueMessage(new MessageInterested());
			} else {
				socket.enqueueMessage(new MessageUninterested());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o == this) {
			return true;
		}

		if (!(o instanceof Peer)) {
			return false;
		}

		Peer other = (Peer) o;

		return Arrays.equals(id, other.id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(id);
	}

	/**
	 * Gets the amount of requests we are allowed to make to this peer
	 *
	 * @return the maximum amount of concurrent requests we can make to this peer
	 */
	public int getRequestLimit() {
		return requestLimit;
	}

	/**
	 * Checks if the client is interested.
	 * @param direction The direction to check for.
	 * @return <code>true</code> when the peer for the given direction is in the choke state.
	 */
	public boolean isInterested(PeerDirection direction) {
		return getClientByDirection(direction).isInterested();
	}

	/**
	 * Checks if the client is choked.
	 * @param direction The direction to check for.
	 * @return <code>true</code> when the peer for the given direction is in the choke state.
	 */
	public boolean isChoked(PeerDirection direction) {
		return getClientByDirection(direction).isChoked();
	}

	private Client getClientByDirection(PeerDirection type) {
		switch (type) {
			case Download:
				return myClient;

			case Upload:
				return peerClient;

			default:
				throw new IllegalArgumentException("Missing enum type: " + type);
		}
	}

	/**
	 * Gets the amount of pieces this peer has
	 *
	 * @return returns the amount of pieces which this peer has
	 */
	public int countHavePieces() {
		return haveState.countHavePieces();
	}

	/**
	 * Gets the socket handler which handles the socket of this peer
	 *
	 * @return The socket associated to this peer.
	 *
	 * @deprecated Will be replaced by alternatives as this class is handling a non-extensible process.
	 */
	@Deprecated
	public BitTorrentSocket getBitTorrentSocket() {
		return socket;
	}

	/**
	 * Requests to queue the next piece in the socket for sending
	 */
	public void queueNextPieceForSending() {
		if (pendingMessages > 0) {
			return;
		}

		Job request = peerClient.popNextJob();
		if (request == null) {
			return;
		}

		addToPendingMessages(1);

		torrent.addDiskJob(new DiskJobReadBlock(request.getPiece(), request.getBlockIndex(), request.getLength(), this::onReadBlockComplete));
	}

	/**
	 * @return The ID of the peer in base-12.
	 */
	public String getIdAsString() {
		return idString;
	}

	private void onReadBlockComplete(DiskJobReadBlock readJob) {
		final byte[] data = readJob.getBlockData();
		socket.enqueueMessage(new MessageBlock(readJob.getPiece().getIndex(), readJob.getOffset(), data));
		addToPendingMessages(-1);
		torrent.addUploadedBytes(data.length);
	}

	public static final class Builder {

		private BitTorrentSocket socket;
		private Torrent torrent;
		byte[] extensionBytes;
		byte[] id;

		public Builder setSocket(BitTorrentSocket socket) {
			this.socket = socket;
			return this;
		}

		public Builder setTorrent(Torrent torrent) {
			this.torrent = torrent;
			return this;
		}

		public Builder setExtensionBytes(byte[] extensionBytes) {
			Argument.requireNonNull(extensionBytes, "Extension bytes can not be null");
			if (extensionBytes.length != 8) {
				throw new IllegalArgumentException("Extension bytes are defined to be 8 bytes. (BEP #03)");
			}

			this.extensionBytes = extensionBytes;
			return this;
		}

		public Builder setId(byte[] id) {
			Argument.requireNonNull(id, "Id can not be null");
			if (id.length != 20) {
				throw new IllegalArgumentException("Id bytes are defined to be 20 bytes. (BEP #03)");
			}

			this.id = id;
			return this;
		}

		public Peer build() {
			return new Peer(this);
		}
	}

}
