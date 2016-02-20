package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageInterested}
 */
@RunWith(EasyMockRunner.class)
public class MessageInterestedTest extends EasyMockSupport {

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

		assertEquals("Incorrect message ID", 2 ,cut.getId());
		assertEquals("Incorrect message length", 1 ,cut.getLength());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageInterested["));
	}

	@Test
	public void testProcess() {
		Peer peerMock = createMock(Peer.class);

		peerMock.setInterested(eq(PeerDirection.Upload), eq(true));

		replayAll();

		MessageInterested cut = new MessageInterested();
		cut.process(peerMock);

		verifyAll();
	}

}