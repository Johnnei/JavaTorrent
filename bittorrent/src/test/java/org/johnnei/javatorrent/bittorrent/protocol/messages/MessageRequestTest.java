package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Job;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageRequest}
 */
@RunWith(EasyMockRunner.class)
public class MessageRequestTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		byte[] expectedBytes = { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 };
		OutStream outStream = new OutStream();

		MessageRequest cut = new MessageRequest(1, 2, 3);

		cut.write(outStream);

		assertEquals("Incorrect message length", 1 + expectedBytes.length, cut.getLength());
		assertArrayEquals("Incorrect output", expectedBytes, outStream.toByteArray());
		assertEquals("Incorrect message ID", 6, cut.getId());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageRequest["));
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream(new byte[] {
				0, 0, 0, 1,
				0, 0, 0, 2,
				0, 0, 0, 3
		});

		Job expectedJob = new Job(1, 2, 3);

		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);
		expect(filesMock.hasPiece(eq(1))).andReturn(true);
		peerMock.addJob(eq(expectedJob), eq(PeerDirection.Upload));

		replayAll();

		MessageRequest cut = new MessageRequest();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessNotHavingPiece() {
		InStream inStream = new InStream(new byte[] {
				0, 0, 0, 1,
				0, 0, 0, 2,
				0, 0, 0, 3
		});

		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);
		expect(filesMock.hasPiece(eq(1))).andReturn(false);
		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		socketMock.close();

		replayAll();

		MessageRequest cut = new MessageRequest();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}
}