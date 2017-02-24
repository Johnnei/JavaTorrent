package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.socket.ISocket;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link UtpPeerConnectionAcceptor}
 */
public class UtpPeerConnectionAcceptorTest {

	@Rule
	public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

	@Test
	public void testAcceptSocket() throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ISocket socketMock = mock(ISocket.class);

		UtpPeerConnectionAcceptor cut = new UtpPeerConnectionAcceptor(torrentClientMock);

		// Get the locks to fake that we reached the timeout
		ReentrantLock lock = Whitebox.getInternalState(cut, "notifyLock");
		Condition wakeUpCondition = Whitebox.getInternalState(cut, "onNewConnection");

		new Thread(() -> {
			await("Wake up condition to be locked").atMost(5, TimeUnit.SECONDS).until(() -> {
				lock.lock();
				try {
					lock.hasWaiters(wakeUpCondition);
				} finally {
					lock.unlock();
				}
			});
			cut.onReceivedConnection(socketMock);
		}).start();

		assertEquals("Incorrect socket has been returned", socketMock, cut.acceptSocket());
	}

}