package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageRequest}
 */
public class MessageRequestTest {

	private MessageRequest cut;
	private Peer peerMock;
	private Torrent torrentMock;
	private Piece pieceMock;
	private MetadataFileSet metadataFileSetMock;
	private InStream inStream;

	@Test
	public void testWrite() {
		MessageRequest cut = new MessageRequest(5);

		OutStream outStream = new OutStream();
		cut.write(outStream);

		byte[] result = outStream.toByteArray();
		String expectedOutput = "d5:piecei5e8:msg_typei0ee";

		assertEquals(expectedOutput.getBytes(Charset.forName("UTF-8")).length, cut.getLength(), "Incorrect length");
		assertEquals(expectedOutput, new String(result, Charset.forName("UTF-8")), "Incorrect output");
	}

	private void prepareSuccessfulRead() {
		cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		peerMock = mock(Peer.class);
		torrentMock = mock(Torrent.class);
		pieceMock = mock(Piece.class);
		Metadata metadataMock = mock(Metadata.class);
		metadataFileSetMock = mock(MetadataFileSet.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.getPiece(0)).thenReturn(pieceMock);
	}

	@Test
	public void testReadAndProcess() {
		prepareSuccessfulRead();

		cut.read(inStream);
		cut.process(peerMock);
	}

	@Test
	public void testOnReadMetadataBlockCompleted() throws IOException {
		prepareSuccessfulRead();
		PeerExtensions extensionsMock = mock(PeerExtensions.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		ArgumentCaptor<IDiskJob> diskJobCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionsMock));
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(extensionsMock.hasExtension(eq("ut_metadata"))).thenReturn(true);
		when(extensionsMock.getExtensionId(eq("ut_metadata"))).thenReturn(3);

		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).addDiskJob(diskJobCapture.capture());
		diskJobCapture.getValue().process();

		verify(socketMock).enqueueMessage(isA(MessageExtension.class));
	}

	@Test
	public void testOnReadMetadataBlockCompletedMissingExtensions() throws IOException {
		prepareSuccessfulRead();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		ArgumentCaptor<IDiskJob> diskJobCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.empty());
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);

		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).addDiskJob(diskJobCapture.capture());
		diskJobCapture.getValue().process();
		verify(socketMock).close();
	}

	@Test
	public void testOnReadMetadataBlockCompletedMissingUtMetadata() throws IOException {
		prepareSuccessfulRead();
		PeerExtensions extensionsMock = mock(PeerExtensions.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		ArgumentCaptor<IDiskJob> diskJobCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionsMock));
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(extensionsMock.hasExtension(eq("ut_metadata"))).thenReturn(false);

		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).addDiskJob(diskJobCapture.capture());
		diskJobCapture.getValue().process();

		verify(socketMock).close();
	}

	@Test
	public void testReadAndProcessDownloadingMetadata() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		PeerExtensions extensionsMock = mock(PeerExtensions.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionsMock));
		when(extensionsMock.hasExtension(eq("ut_metadata"))).thenReturn(true);
		when(extensionsMock.getExtensionId(eq("ut_metadata"))).thenReturn(3);

		cut.read(inStream);
		cut.process(peerMock);

		verify(socketMock).enqueueMessage(isA(MessageExtension.class));
	}

	@Test
	public void testReadAndProcessDownloadingMetadataNoExtensions() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.empty());

		cut.read(inStream);
		cut.process(peerMock);
	}

	@Test
	public void testReadAndProcessDownloadingMetadataNoUtMetadata() {
		MessageRequest cut = new MessageRequest(5);

		final String input = "d8:msg_typei0e5:piecei5ee";
		InStream inStream = new InStream(input.getBytes(Charset.forName("UTF-8")));

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		PeerExtensions extensionsMock = mock(PeerExtensions.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionsMock));
		when(extensionsMock.hasExtension(eq("ut_metadata"))).thenReturn(false);

		cut.read(inStream);
		cut.process(peerMock);
	}

	@Test
	public void testToString() {
		MessageRequest cut = new MessageRequest();
		assertTrue(cut.toString().startsWith("MessageRequest["), "Incorrect toString start");
	}

}
