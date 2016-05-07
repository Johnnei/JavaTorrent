package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.AbstractPeerConnectionAcceptor;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.UtpSocket;

/**
 * Created by johnn on 14/05/2016.
 */
public class UtpPeerConnectionAcceptor extends AbstractPeerConnectionAcceptor {

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onNewConnection = notifyLock.newCondition();

	private Queue<UtpSocket> socketQueue;

	public UtpPeerConnectionAcceptor(TorrentClient torrentClient) {
		super(torrentClient);
		socketQueue = new LinkedList<>();
	}

	public void onReceivedConnection(UtpSocket socket) {
		synchronized (this) {
			socketQueue.add(socket);
		}

		notifyLock.lock();
		try {
			onNewConnection.signalAll();
		} finally {
			notifyLock.unlock();
		}
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
