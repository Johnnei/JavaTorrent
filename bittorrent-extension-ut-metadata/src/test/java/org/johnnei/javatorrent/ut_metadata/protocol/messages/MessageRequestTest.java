package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageRequest}
 */
public class MessageRequestTest extends EasyMockSupport {

	private MessageRequest cut;
	private Peer peerMock;
	private Torrent torrentMock;
	private Piece pieceMock;
	private MetadataFileSet metadataMock;
	private InStream inStream;

	@Test
	public void testWrite() {
		MessageRequest cut = new MessageRequest(5);

		OutStream outStream = new OutStream();
		cut.write(outStream);

		byte[] result = outStream.toByteArray();
		String expectedOutput = "d5:piecei5e8:msg_typei0ee";

		assertEquals("Incorrect length", expectedOutput.getBytes(Charset.forName("UTF-8")).length, cut.getLength());
		assertEquals("Incorrect output", expectedOutput, new String(result, Charset.forName("UTF-8")));
	}

	private void prepareSuccessfulRead() {
		cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		peerMock = createNiceMock(Peer.class);
		torrentMock = createNiceMock(Torrent.class);
		pieceMock = createNiceMock(Piece.class);
		metadataMock = createNiceMock(MetadataFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.getMetadata()).andReturn(Optional.of(metadataMock));
		expect(metadataMock.getPiece(0)).andReturn(pieceMock);
	}

	@Test
	public void testReadAndProcess() {
		prepareSuccessfulRead();

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testOnReadMetadataBlockCompleted() throws IOException {
		prepareSuccessfulRead();
		PeerExtensions extensionsMock = createNiceMock(PeerExtensions.class);
		BitTorrentSocket socketMock = createNiceMock(BitTorrentSocket.class);

		Capture<IDiskJob> diskJobCapture = EasyMock.newCapture();
		torrentMock.addDiskJob(and(isA(DiskJobReadBlock.class), capture(diskJobCapture)));

		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionsMock));
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		expect(extensionsMock.hasExtension(eq("ut_metadata"))).andReturn(true);
		expect(extensionsMock.getExtensionId(eq("ut_metadata"))).andReturn(3);
		socketMock.enqueueMessage(isA(MessageExtension.class));

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		diskJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testOnReadMetadataBlockCompletedMissingExtensions() throws IOException {
		prepareSuccessfulRead();
		BitTorrentSocket socketMock = createNiceMock(BitTorrentSocket.class);

		Capture<IDiskJob> diskJobCapture = EasyMock.newCapture();
		torrentMock.addDiskJob(and(isA(DiskJobReadBlock.class), capture(diskJobCapture)));

		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.empty());
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		socketMock.close();

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		diskJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testOnReadMetadataBlockCompletedMissingUtMetadata() throws IOException {
		prepareSuccessfulRead();
		PeerExtensions extensionsMock = createNiceMock(PeerExtensions.class);
		BitTorrentSocket socketMock = createNiceMock(BitTorrentSocket.class);

		Capture<IDiskJob> diskJobCapture = EasyMock.newCapture();
		torrentMock.addDiskJob(and(isA(DiskJobReadBlock.class), capture(diskJobCapture)));

		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionsMock));
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		expect(extensionsMock.hasExtension(eq("ut_metadata"))).andReturn(false);
		socketMock.close();

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		diskJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testReadAndProcessDownloadingMetadata() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createNiceMock(Torrent.class);
		PeerExtensions extensionsMock = createNiceMock(PeerExtensions.class);
		BitTorrentSocket socketMock = createNiceMock(BitTorrentSocket.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);
		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionsMock));
		expect(extensionsMock.hasExtension(eq("ut_metadata"))).andReturn(true);
		expect(extensionsMock.getExtensionId(eq("ut_metadata"))).andReturn(3);
		socketMock.enqueueMessage(isA(MessageExtension.class));

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessDownloadingMetadataNoExtensions() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createNiceMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);
		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.empty());

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessDownloadingMetadataNoUtMetadata() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createNiceMock(Torrent.class);
		PeerExtensions extensionsMock = createNiceMock(PeerExtensions.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);
		expect(peerMock.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionsMock));
		expect(extensionsMock.hasExtension(eq("ut_metadata"))).andReturn(false);

		replayAll();

		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testToString() {
		MessageRequest cut = new MessageRequest();
		assertTrue("Incorrect toString start", cut.toString().startsWith("MessageRequest["));
	}

}