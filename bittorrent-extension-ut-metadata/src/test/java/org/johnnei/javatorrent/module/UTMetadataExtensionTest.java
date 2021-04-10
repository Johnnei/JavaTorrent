package org.johnnei.javatorrent.module;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageData;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageReject;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageUnknown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UTMetadataExtension}
 */
public class UTMetadataExtensionTest {

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	private UTMetadataExtension cut;

	private Path torrentFileFolder;
	private Path downloadFolder;

	@BeforeEach
	public void setUp(@TempDir Path tmp) {
		torrentFileFolder = tmp.resolve("a");
		downloadFolder = tmp.resolve("b");
		cut = new UTMetadataExtension(torrentFileFolder, downloadFolder);
	}

	@Test
	public void testGetExtensionName(@TempDir Path tmp) throws IOException {
		assertEquals("ut_metadata", new UTMetadataExtension(tmp.resolve("a"), tmp.resolve("b")).getExtensionName());
	}

	@Test
	public void testProcessHandshakeMetadataNoMetadataSize() throws IOException {
		Peer peerMock = mock(Peer.class);

		BencodedMap handshakeDictionary = new BencodedMap();
		BencodedMap mEntry = new BencodedMap();

		handshakeDictionary.put("m", mEntry);

		cut.processHandshakeMetadata(peerMock, handshakeDictionary, mEntry);
	}

	@Test
	public void testProcessHandshakeMetadata() throws IOException {
		Peer peerMock = mock(Peer.class);

		BencodedMap handshakeDictionary = new BencodedMap();
		BencodedMap mEntry = new BencodedMap();

		handshakeDictionary.put("metadata_size", new BencodedInteger(512));
		handshakeDictionary.put("m", mEntry);

		ArgumentCaptor<MetadataInformation> informationCapture = ArgumentCaptor.forClass(MetadataInformation.class);

		cut.processHandshakeMetadata(peerMock, handshakeDictionary, mEntry);

		verify(peerMock).addModuleInfo(informationCapture.capture());

		MetadataInformation metaInfo = informationCapture.getValue();
		assertEquals(512, metaInfo.getMetadataSize(), "Incorrect metadata size");
	}

	@Test
	public void testAddHandshakeMetadataDownloadingMetadata() throws IOException {
		Torrent torrent = mock(Torrent.class);
		Peer peerMock = mock(Peer.class);
		BencodedMap bencodedMapMock = mock(BencodedMap.class);

		when(peerMock.getTorrent()).thenReturn(torrent);
		when(torrent.isDownloadingMetadata()).thenReturn(true);

		cut.addHandshakeMetadata(peerMock, bencodedMapMock);
	}

	@Test
	public void testAddHandshakeMetadata() throws IOException {
		Torrent torrent = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		Peer peerMock = mock(Peer.class);
		BencodedMap bencodedMapMock = mock(BencodedMap.class);
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);

		when(peerMock.getTorrent()).thenReturn(torrent);
		when(torrent.isDownloadingMetadata()).thenReturn(false);
		when(torrent.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.getTotalFileSize()).thenReturn(512L);

		cut.addHandshakeMetadata(peerMock, bencodedMapMock);

		verify(bencodedMapMock).put("metadata_size", new BencodedInteger(512));
	}

	@Test
	public void testGetMessageReject() throws IOException {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(UTF8));

		IMessage message = cut.getMessage(inStream);

		assertNotNull(message, "A message should have been returned");
		assertTrue(message instanceof MessageReject, "Incorrect message type returned. Expected: MessageReject.");
	}

	@Test
	public void testGetMessageRequest()  throws IOException {
		InStream inStream = new InStream("d8:msg_typei0e5:piecei5ee".getBytes(UTF8));

		IMessage message = cut.getMessage(inStream);

		assertNotNull(message, "A message should have been returned");
		assertTrue(message instanceof MessageRequest, "Incorrect message type returned. Expected: MessageRequest.");
	}

	@Test
	public void testGetMessageUnknown() throws IOException {
		InStream inStream = new InStream("d8:msg_typei151e5:piecei42ee".getBytes(UTF8));

		IMessage message = cut.getMessage(inStream);

		assertNotNull(message, "A message should have been returned");
		assertTrue(message instanceof MessageUnknown, "Incorrect message type returned. Expected: MessageUnknown.");
	}

	@Test
	public void testGetMessageData() throws IOException {
		InStream inStream = new InStream("d8:msg_typei1e5:piecei42ee".getBytes(UTF8));

		IMessage message = cut.getMessage(inStream);

		assertNotNull(message, "A message should have been returned");
		assertTrue(message instanceof MessageData, "Incorrect message type returned. Expected: MessageData.");
	}

	@Test
	public void testGetDownloadFolder() throws IOException {
		assertEquals(downloadFolder, cut.getDownloadFolder(), "Incorrect location");
	}

	@Test
	public void testGetTorrentFile() throws IOException {
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);

		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHashString()).thenReturn("c8369f0ba4bf6cd87fb13b3437782e2c7820bb38");

		Path torrentFile = cut.getTorrentFile(torrentMock);

		assertEquals(torrentFileFolder.resolve("c8369f0ba4bf6cd87fb13b3437782e2c7820bb38.torrent"), torrentFile, "Incorrect location");
	}

}
