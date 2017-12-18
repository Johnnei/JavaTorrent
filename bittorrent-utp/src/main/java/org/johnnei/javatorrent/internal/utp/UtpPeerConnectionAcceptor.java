package org.johnnei.javatorrent.internal.utp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;

/**
 * Accepts connection which have been detected as a new connection in {@link UtpMultiplexer}
 */
public class UtpPeerConnectionAcceptor implements Runnable {

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onNewConnection = notifyLock.newCondition();

	private final Collection<UtpSocket> socketQueue;

	private final TorrentClient torrentClient;

	/**
	 * Creates a new uTP acceptor
	 * @param torrentClient The torrent client to which we are accepting peers.
	 */
	public UtpPeerConnectionAcceptor(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		socketQueue = new LinkedList<>();
	}

	/**
	 * Adds a newly found uTP connection to the queue to be connected.
	 * @param socket The socket to add.
	 */
	public void onReceivedConnection(UtpSocket socket) {
		synchronized (this) {
			socketQueue.add(socket);
		}

		Sync.signalAll(notifyLock, onNewConnection);
	}

	/**
	 * FIXME: Run should be able to run on the Executor Pool instead of a dedicated thread.
	 */
	@Override
	public void run() {
		notifyLock.lock();

		Optional<UtpSocket> connectedSocket;
		try {
			while (!hasPendingConnection()) {
				onNewConnection.await();
			}

			while (!(connectedSocket = getEstablishedConnection()).isPresent()) {
				// Check the condition every once in a while to check that the socket has become connected.
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for a new connection", e);
		} finally {
			notifyLock.unlock();
		}

		synchronized (this) {
			socketQueue.remove(connectedSocket.get());
			torrentClient.getHandshakeHandler().onConnectionReceived(connectedSocket.get().getChannel());
		}
	}

	private synchronized boolean hasPendingConnection() {
		return !socketQueue.isEmpty();
	}

	private synchronized Optional<UtpSocket> getEstablishedConnection() {
		return socketQueue.stream().filter(socket -> socket.getConnectionState() == ConnectionState.CONNECTED).findAny();
	}
}
