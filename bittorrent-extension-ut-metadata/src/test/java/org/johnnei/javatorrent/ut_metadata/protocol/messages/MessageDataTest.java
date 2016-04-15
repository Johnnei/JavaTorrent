package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.nio.charset.Charset;
import java.util.Optional;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageData}
 */
public class MessageDataTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		byte[] expectedOutput = new byte[] { 0x32, 0x56, 0x74 };

		OutStream outStream = new OutStream();

		MessageData cut = new MessageData(42, expectedOutput);

		assertEquals("Incorrect length. 26 bytes for dictionary + 3 for data", 29, cut.getLength());

		cut.write(outStream);

		String expectedDictionary = "d8:msg_typei1e5:piecei42ee";
		String dictionary = new String(outStream.toByteArray(), 0, 26, Charset.forName("UTF-8"));
		assertEquals("Incorrect dictionary part", expectedDictionary, dictionary);

		byte[] output = outStream.toByteArray();
		byte[] data = new byte[3];
		data[0] = output[26];
		data[1] = output[27];
		data[2] = output[28];
		assertArrayEquals("Incorrect data part", expectedOutput, data);
	}

	@Test
	public void testReadAndProcess() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		MetadataFileSet metadataMock = createNiceMock(MetadataFileSet.class);

		expect(metadataMock.getBlockSize()).andReturn(16384);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);
		expect(torrentMock.getMetadata()).andReturn(Optional.of(metadataMock));
		expect(peerMock.getTorrent()).andStubReturn(torrentMock);

		torrentMock.onReceivedBlock(eq(metadataMock), eq(0), eq(42 * 16384), aryEq(dataBytes));
		peerMock.onReceivedBlock(0, 42);

		replayAll();

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessMetadataCompleted() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(torrentMock.isDownloadingMetadata()).andReturn(false);

		replayAll();

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadAndProcessMissingMetadata() {
		byte[] dictionaryBytes = "d8:msg_typei1e5:piecei42ee".getBytes(Charset.forName("UTF-8"));
		byte[] dataBytes = new byte[] { 0x32, 0x56, 0x74 };
		byte[] input = new byte[dictionaryBytes.length + dataBytes.length];
		TestUtils.copySection(dictionaryBytes, input, 0);
		TestUtils.copySection(dataBytes, input, dictionaryBytes.length);

		Peer peerMock = createNiceMock(Peer.class);
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		expect(peerMock.getTorrent()).andStubReturn(torrentMock);
		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);
		expect(torrentMock.getMetadata()).andReturn(Optional.empty());
		socketMock.close();

		replayAll();

		MessageData cut = new MessageData();
		cut.read(new InStream(input));
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testToString() {
		MessageData cut = new MessageData();
		assertTrue("Incorrect toString start", cut.toString().startsWith("MessageData["));
	}

}