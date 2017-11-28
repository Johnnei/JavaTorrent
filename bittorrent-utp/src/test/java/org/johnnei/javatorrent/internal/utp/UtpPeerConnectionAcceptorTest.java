package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpPeerConnectionAcceptor}
 */
public class UtpPeerConnectionAcceptorTest {

	@Test
	public void testAcceptSocket() throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		UtpSocket socketMock = mock(UtpSocket.class);

		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

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

		assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> assertEquals(socketMock, cut.acceptSocket(), "Incorrect socket has been returned"));
	}

}
