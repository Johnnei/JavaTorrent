package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link MessageBlock}
 */
@RunWith(EasyMockRunner.class)
public class MessageBlockTest extends EasyMockSupport {

	@Test
	public void testStaticMethods() {
		MessageBlock cut = new MessageBlock();

		assertEquals("Incorrect message ID", 7 ,cut.getId());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageBlock["));

		cut = new MessageBlock(1, 2, new byte[] {});
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageBlock["));
	}

	@Test
	public void testProcessQuickReceive() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		}, Duration.ofMillis(200));
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		peerMock.onReceivedBlock(eq(5), eq(0x3800));
		peerMock.addStrike(-1);
		peerMock.setRequestLimit(5);

		replayAll();

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testProcessInvalidLength() {
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);

		peerMock.onReceivedBlock(eq(5), eq(0x37FF));
		peerMock.addStrike(1);

		replayAll();

		MessageBlock cut = new MessageBlock(5, 0x37FF, new byte[] {});
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		});
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		peerMock.onReceivedBlock(eq(5), eq(0x3800));
		peerMock.addStrike(-1);
		expect(peerMock.getRequestLimit()).andStubReturn(2);
		peerMock.setRequestLimit(6);

		replayAll();

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessHighTrust() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		});
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		peerMock.onReceivedBlock(eq(5), eq(0x3800));
		peerMock.addStrike(-1);
		expect(peerMock.getRequestLimit()).andStubReturn(5);

		replayAll();

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testWrite() {
		MessageBlock cut = new MessageBlock(1, 2, new byte[] { 0x00, 0x00, 0x00 });

		byte[] expectedOutput = new byte[] {
				0x00, 0x00, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x02,
				0x00, 0x00, 0x00
		};

		OutStream outStream = new OutStream();
		cut.write(outStream);

		assertEquals("Incorrect message length", 12 ,cut.getLength());
		assertArrayEquals("Incorrect output", expectedOutput, outStream.toByteArray());
	}

}