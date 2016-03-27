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
 * Tests {@link MessageUnchoke}
 */
@RunWith(EasyMockRunner.class)
public class MessageUnchokeTest extends EasyMockSupport {

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

		assertEquals("Incorrect message ID", 1 ,cut.getId());
		assertEquals("Incorrect message length", 1 ,cut.getLength());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageUnchoke["));
	}

	@Test
	public void testProcess() {
		Peer peerMock = createMock(Peer.class);

		peerMock.setChoked(eq(PeerDirection.Download), eq(false));

		replayAll();

		MessageUnchoke cut = new MessageUnchoke();
		cut.process(peerMock);

		verifyAll();
	}

}