package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.network.AbstractPeerConnectionAcceptor;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.UtpSocket;

/**
 * Accepts connection which have been detected as a new connection in {@link org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer}
 */
public class UtpPeerConnectionAcceptor extends AbstractPeerConnectionAcceptor {

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onNewConnection = notifyLock.newCondition();

	private final Queue<UtpSocket> socketQueue;

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
		try {
			while (!hasPendingConnection()) {
				onNewConnection.await();
			}
		} catch (InterruptedException e) {
			throw new IOException("Interrupted while waiting for a new connection", e);
		} finally {
			notifyLock.unlock();
		}

		synchronized (this) {
			return socketQueue.poll();
		}
	}

	private synchronized boolean hasPendingConnection() {
		return !socketQueue.isEmpty();
	}
}
