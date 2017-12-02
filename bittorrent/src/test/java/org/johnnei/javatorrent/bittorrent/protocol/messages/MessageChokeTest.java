package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MessageChoke}
 */
public class MessageChokeTest {

	@Test
	public void testEmptyMethods() {
		MessageChoke cut = new MessageChoke();

		// Pass null to the method to cause exceptions if they do use it
		cut.write(null);
		cut.read(null);
	}

	@Test
	public void testStaticMethods() {
		MessageChoke cut = new MessageChoke();

		assertEquals(0 ,cut.getId(), "Incorrect message ID");
		assertEquals(1 ,cut.getLength(), "Incorrect message length");
		assertTrue(cut.toString().startsWith("MessageChoke["), "Incorrect toString start.");
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);

		MessageChoke cut = new MessageChoke();
		cut.process(peerMock);

		verify(peerMock).setChoked(PeerDirection.Download, true);
		verify(peerMock).discardAllBlockRequests();
	}

}
