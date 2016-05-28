package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.socket.UtpSocket;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link UtpPeerConnectionAcceptor}
 */
public class UtpPeerConnectionAcceptorTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpPeerConnectionAcceptorTest.class);

	@Rule
	public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

	@Test
	public void testAcceptSocket() throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		UtpSocket socketMock = mock(UtpSocket.class);

		UtpPeerConnectionAcceptor cut = new UtpPeerConnectionAcceptor(torrentClientMock);
		new Thread(() -> {
			try {
				Thread.sleep(100);
				cut.onReceivedConnection(socketMock);
			} catch (InterruptedException e) {
				LOGGER.info("Test got interrupted, test  might fail if the wait time was too short");
			}
		}).start();

		assertEquals("Incorrect socket has been returned", socketMock, cut.acceptSocket());
	}

}