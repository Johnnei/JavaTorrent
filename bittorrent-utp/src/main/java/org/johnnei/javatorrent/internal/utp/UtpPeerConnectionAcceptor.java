package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.network.AbstractPeerConnectionAcceptor;

/**
 * Accepts connection which have been detected as a new connection in {@link UtpMultiplexer}
 */
public class UtpPeerConnectionAcceptor extends AbstractPeerConnectionAcceptor {

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onNewConnection = notifyLock.newCondition();

	private final Collection<UtpSocket> socketQueue;

	/**
	 * Creates a new uTP acceptor
	 * @param torrentClient The torrent client to which we are accepting peers.
	 */
	public UtpPeerConnectionAcceptor(TorrentClient torrentClient) {
		super(torrentClient);
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

	@Override
	protected ISocket acceptSocket() throws IOException {
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
			throw new IOException("Interrupted while waiting for a new connection", e);
		} finally {
			notifyLock.unlock();
		}

		synchronized (this) {
			socketQueue.remove(connectedSocket.get());
			return connectedSocket.get();
		}
	}

	private synchronized boolean hasPendingConnection() {
		return !socketQueue.isEmpty();
	}

	private synchronized Optional<UtpSocket> getEstablishedConnection() {
		return socketQueue.stream().filter(socket -> socket.getConnectionState() == ConnectionState.CONNECTED).findAny();
	}
}
