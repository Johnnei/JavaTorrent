package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MessageInterested}
 */
public class MessageInterestedTest {

	@Test
	public void testEmptyMethods() {
		MessageInterested cut = new MessageInterested();

		// Pass null to the method to cause exceptions if they do use it
		cut.write(null);
		cut.read(null);
	}

	@Test
	public void testStaticMethods() {
		MessageInterested cut = new MessageInterested();

		assertEquals(2 ,cut.getId(), "Incorrect message ID");
		assertEquals(1 ,cut.getLength(), "Incorrect message length");
		assertTrue(cut.toString().startsWith("MessageInterested["), "Incorrect toString start.");
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);

		MessageInterested cut = new MessageInterested();
		cut.process(peerMock);

		verify(peerMock).setInterested(PeerDirection.Upload, true);
	}

}
