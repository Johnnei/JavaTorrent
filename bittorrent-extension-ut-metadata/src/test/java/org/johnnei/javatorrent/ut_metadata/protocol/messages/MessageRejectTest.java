package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.nio.charset.Charset;
import java.util.Optional;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageReject}
 */
public class MessageRejectTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		MessageReject cut = new MessageReject(5);

		OutStream outStream = new OutStream();
		cut.write(outStream);

		byte[] result = outStream.toByteArray();
		String expectedOutput = "d8:msg_typei2e5:piecei5ee";

		assertEquals("Incorrect length", expectedOutput.getBytes(Charset.forName("UTF-8")).length, cut.getLength());
		assertEquals("Incorrect output", expectedOutput, new String(result, Charset.forName("UTF-8")));
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(Charset.forName("UTF-8")));

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createNiceMock(Torrent.class);
		MetadataFileSet metadataFileSetMock = createNiceMock(MetadataFileSet.class);
		Piece pieceMock = createNiceMock(Piece.class);

		expect(peerMock.getTorrent()).andReturn(torrentMock);
		expect(torrentMock.getMetadata()).andReturn(Optional.of(metadataFileSetMock));
		expect(metadataFileSetMock.getPiece(0)).andReturn(pieceMock);
		pieceMock.setBlockStatus(5, BlockStatus.Needed);
		peerMock.onReceivedBlock(0, 5);

		replayAll();

		MessageReject cut = new MessageReject();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessNoMetadata() {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(Charset.forName("UTF-8")));

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createNiceMock(Torrent.class);
		BitTorrentSocket socketMock = createNiceMock(BitTorrentSocket.class);

		expect(peerMock.getTorrent()).andReturn(torrentMock);
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		expect(torrentMock.getMetadata()).andReturn(Optional.empty());
		socketMock.close();

		replayAll();

		MessageReject cut = new MessageReject();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testToString() {
		MessageReject cut = new MessageReject();
		assertTrue("Incorrect toString start", cut.toString().startsWith("MessageReject["));
	}

}