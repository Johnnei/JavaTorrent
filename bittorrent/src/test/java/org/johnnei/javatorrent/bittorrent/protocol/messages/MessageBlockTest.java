package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;

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
		Job expectedJob = new Job(5, 1);
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		expect(filesMock.getBlockSize()).andReturn(0x37FF);
		peerMock.removeJob(eq(expectedJob), eq(PeerDirection.Download));
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
		Job expectedJob = new Job(5, 1);
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		expect(filesMock.getBlockSize()).andReturn(0x37FF);
		peerMock.removeJob(eq(expectedJob), eq(PeerDirection.Download));
		peerMock.addStrike(1);

		replayAll();

		MessageBlock cut = new MessageBlock(5, 0x3800, new byte[] {});
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
		Job expectedJob = new Job(5, 1);
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		expect(filesMock.getBlockSize()).andReturn(0x37FF);
		peerMock.removeJob(eq(expectedJob), eq(PeerDirection.Download));
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
		Job expectedJob = new Job(5, 1);
		Peer peerMock = createMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		torrentMock.collectPiece(eq(5), eq(0x3800), aryEq(new byte[] { 0x00 }));
		expect(filesMock.getBlockSize()).andReturn(0x37FF);
		peerMock.removeJob(eq(expectedJob), eq(PeerDirection.Download));
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