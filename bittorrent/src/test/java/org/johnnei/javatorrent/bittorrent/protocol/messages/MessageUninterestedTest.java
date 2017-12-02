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
 * Tests {@link MessageUninterested}
 */
public class MessageUninterestedTest {

	@Test
	public void testEmptyMethods() {
		MessageUninterested cut = new MessageUninterested();

		// Pass null to the method to cause exceptions if they do use it
		cut.write(null);
		cut.read(null);
	}

	@Test
	public void testStaticMethods() {
		MessageUninterested cut = new MessageUninterested();

		assertEquals(3 ,cut.getId(), "Incorrect message ID");
		assertEquals(1 ,cut.getLength(), "Incorrect message length");
		assertTrue(cut.toString().startsWith("MessageUninterested["), "Incorrect toString start.");
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);

		MessageUninterested cut = new MessageUninterested();
		cut.process(peerMock);

		verify(peerMock).setInterested(eq(PeerDirection.Upload), eq(false));
	}

}
