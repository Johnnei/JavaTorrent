package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MessageUnchoke}
 */
public class MessageUnchokeTest {

	@Test
	public void testEmptyMethods() {
		MessageUnchoke cut = new MessageUnchoke();

		// Pass null to the method to cause exceptions if they do use it
		cut.write(null);
		cut.read(null);
	}

	@Test
	public void testStaticMethods() {
		MessageUnchoke cut = new MessageUnchoke();

		assertEquals(1 ,cut.getId(), "Incorrect message ID");
		assertEquals(1 ,cut.getLength(), "Incorrect message length");
		assertTrue(cut.toString().startsWith("MessageUnchoke["), "Incorrect toString start.");
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);

		MessageUnchoke cut = new MessageUnchoke();
		cut.process(peerMock);

		verify(peerMock).setChoked(eq(PeerDirection.Download), eq(false));
	}

}
