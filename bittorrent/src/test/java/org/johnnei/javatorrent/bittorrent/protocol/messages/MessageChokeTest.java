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
 * Tests {@link MessageChoke}
 */
@RunWith(EasyMockRunner.class)
public class MessageChokeTest extends EasyMockSupport {

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

		assertEquals("Incorrect message ID", 0 ,cut.getId());
		assertEquals("Incorrect message length", 1 ,cut.getLength());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageChoke["));
	}

	@Test
	public void testProcess() {
		Peer peerMock = createMock(Peer.class);

		peerMock.setChoked(eq(PeerDirection.Download), eq(true));
		peerMock.discardAllBlockRequests();

		replayAll();

		MessageChoke cut = new MessageChoke();
		cut.process(peerMock);

		verifyAll();
	}

}