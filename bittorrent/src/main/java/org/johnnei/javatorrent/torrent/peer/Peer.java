package org.johnnei.javatorrent.torrent.peer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.disk.DiskJob;
import org.johnnei.javatorrent.disk.DiskJobSendBlock;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.utils.JMath;

public class Peer implements Comparable<Peer> {

	private Torrent torrent;

	/**
	 * Client information about the connected peer
	 * This will contain the requests the endpoint made of us
	 */
	private Client peerClient;

	/**
	 * Client information about me retrieved from the connected peer<br/>
	 * This will contain the requests we made to the endpoint
	 */
	private Client myClient;

	private String clientName;

	/**
	 * The count of messages which are still being processed by the IOManager
	 */
	private int pendingMessages;

	/**
	 * The amount of errors the client made<br/>
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
	private byte[] extensionBytes;

	/**
	 * The absolute maximum amount of requests which the peer can support<br/>
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
	private BitTorrentSocket socket;

	/**
	 * A map of the extra data stored by {@link IModule} which are peer specific
	 */
	private Map<Class<?>, Object> extensions;

	public Peer(BitTorrentSocket client, Torrent torrent, byte[] extensionBytes) {
		this.torrent = torrent;
		this.socket = client;
		this.extensionBytes = extensionBytes;
		peerClient = new Client();
		myClient = new Client();
		extensions = new HashMap<>();
		clientName = "pending";
		absoluteRequestLimit = Integer.MAX_VALUE;
		if (torrent.getFiles() != null) {
			haveState = new Bitfield(JMath.ceilDivision(torrent.getFiles().getPieceCount(), 8));
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
	 * @param index
	 * @param bit
	 * @return returns true if the extension bit is set. Otherwise false
	 */
	public boolean hasExtension(int index, int bit) {
		if (index < 0 || index >= extensionBytes.length) {
			return false;
		}

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
	 * Adds a download or upload job to the peer
	 *
	 * @param job
	 * @param type
	 */
	public void addJob(Job job, PeerDirection type) {
		getClientByDirection(type).addJob(job);
	}

	/**
	 * Removes the download or upload job from the peer
	 *
	 * @param job
	 * @param type
	 */
	public void removeJob(Job job, PeerDirection type) {
		getClientByDirection(type).removeJob(job);
	}

	/**
	 * Add a value to the pending Messages count
	 *
	 * @param i The count to add
	 */
	public synchronized void addToPendingMessages(int i) {
		pendingMessages += i;
	}

	@Override
	public String toString() {
		if (socket != null) {
			return socket.toString();
		} else {
			return "UNCONNECTED";
		}
	}

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
		if (torrent.isDownloadingMetadata()) {
			haveState.setSize(JMath.ceilDivision(torrent.getFiles().getPieceCount(), 8));
		}
	}

	public int getFreeWorkTime() {
		return Math.max(0, getRequestLimit() - getWorkQueueSize(PeerDirection.Download));
	}

	/**
	 * Gets the amount of pieces the client still needs to send
	 *
	 * @return
	 */
	public int getWorkQueueSize(PeerDirection direction) {
		return getClientByDirection(direction).getQueueSize();
	}

	/**
	 * Cancels all pieces
	 */
	public void cancelAllPieces() {
		synchronized (this) {
			if (getWorkQueueSize(PeerDirection.Download) > 0) {
				Object[] keys = myClient.getKeySet().toArray();
				for (int i = 0; i < keys.length; i++) {
					Job job = (Job) keys[i];
					torrent.getFiles().getPiece(job.getPieceIndex()).reset(job.getBlockIndex());
				}
				myClient.clearJobs();
			}
		}
	}

	/**
	 * Registers that this peer has the given piece
	 *
	 * @param pieceIndex the piece to marked as "have"
	 */
	public void havePiece(int pieceIndex) {
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

	public Torrent getTorrent() {
		return torrent;
	}

	@Override
	public int compareTo(Peer p) {
		int myValue = getCompareValue();
		int theirValue = p.getCompareValue();

		return myValue - theirValue;
	}

	private int getCompareValue() {
		return (getWorkQueueSize(PeerDirection.Download) * 5000) + haveState.countHavePieces() + socket.getDownloadRate();
	}

	/**
	 * Adds an amount of strikes to the peer
	 *
	 * @param i
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
	}

	/**
	 * Sets the amount of requests we think this peer can handle properly.<br/>
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

		return Objects.equals(socket, other.socket);
	}

	@Override
	public int hashCode() {
		return Objects.hash(socket);
	}

	/**
	 * Gets the amount of requests we are allowed to make to this peer
	 *
	 * @return the maximum amount of concurrent requests we can make to this peer
	 */
	public int getRequestLimit() {
		return requestLimit;
	}

	public boolean isInterested(PeerDirection direction) {
		return getClientByDirection(direction).isInterested();
	}

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
		if (haveState == null) {
			return 0;
		}
		return haveState.countHavePieces();
	}

	/**
	 * Gets the socket handler which handles the socket of this peer
	 *
	 * @return
	 *
	 */
	public BitTorrentSocket getBitTorrentSocket() {
		return socket;
	}

	/**
	 * Requests to queue the next piece in the socket for sending
	 */
	public void queueNextPieceForSending() {
		if (myClient.getQueueSize() == 0 || pendingMessages > 0) {
			return;
		}

		Job request = peerClient.getNextJob();
		peerClient.removeJob(request);
		addToPendingMessages(1);

		DiskJob sendBlock = new DiskJobSendBlock(this,
				request.getPieceIndex(), request.getBlockIndex(),
				request.getLength()
		);

		torrent.addDiskJob(sendBlock);
	}

}
