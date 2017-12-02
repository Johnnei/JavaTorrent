package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import java.nio.charset.Charset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageData}
 */
public class MessageDataTest {

	@Test
	public void testWrite() {
		byte[] expectedOutput = new byte[] { 0x32, 0x56, 0x74 };

		OutStream outStream = new OutStream();

		MessageData cut = new MessageData(42, expectedOutput);

		assertEquals(29, cut.getLength(), "Incorrect length. 26 bytes for dictionary + 3 for data");

		cut.write(outStream);

		String expectedDictionary = "d5:piecei42e8:msg_typei1ee";
		String dictionary = new String(outStream.toByteArray(), 0, 26, Charset.forName("UTF-8"));
		assertEquals(expectedDictionary, dictionary, "Incorrect dictionary part");

		byte[] output = outStream.toByteArray();
		byte[] data = new byte[3];
		data[0] = output[26];
		data[1] = output[27];
		data[2] = output[28];
		assertArrayEquals(expectedOutput, data, "Incorrect data part");
	}

	@Test
	public void testReadAndProcess() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);

		when(metadataFileSetMock.getBlockSize()).thenReturn(16384);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(peerMock.getTorrent()).thenReturn(torrentMock);

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);

		verify(torrentMock).onReceivedBlock(eq(metadataFileSetMock), eq(0), eq(42 * 16384), aryEq(dataBytes));
	}

	@Test
	public void testReadAndProcessMetadataCompleted() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(false);

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);
	}

	@Test
	public void testReadAndProcessMissingMetadata() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Metadata metadataMock = mock(Metadata.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);

		verify(socketMock).close();
	}

	@Test
	public void testToString() {
		MessageData cut = new MessageData();
		assertTrue(cut.toString().startsWith("MessageData["), "Incorrect toString start");
	}

}
