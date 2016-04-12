package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.Bencoder;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link UTMetadataExtension}
 */
public class UTMetadataExtensionTest extends EasyMockSupport {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Test
	public void testGetExtensionName() {
		assertEquals("ut_metadata", new UTMetadataExtension().getExtensionName());
	}

	@Test
	public void testProcessHandshakeMetadataNoMetadataSize() {
		Peer peerMock = createMock(Peer.class);

		Map<String, Object> handshakeDictionary = new HashMap<>();
		Map<String, Object> emptyDictionary = Collections.emptyMap();
		handshakeDictionary.put("m", emptyDictionary);

		replayAll();

		UTMetadataExtension cut = new UTMetadataExtension();
		cut.processHandshakeMetadata(peerMock, handshakeDictionary, emptyDictionary);

		verifyAll();
	}

	@Test
	public void testProcessHandshakeMetadata() {
		Peer peerMock = createMock(Peer.class);

		Map<String, Object> handshakeDictionary = new HashMap<>();
		Map<String, Object> emptyDictionary = Collections.emptyMap();
		handshakeDictionary.put("metadata_size", 512);
		handshakeDictionary.put("m", emptyDictionary);

		Capture<MetadataInformation> informationCapture = EasyMock.newCapture();
		peerMock.addModuleInfo(capture(informationCapture));

		replayAll();

		UTMetadataExtension cut = new UTMetadataExtension();
		cut.processHandshakeMetadata(peerMock, handshakeDictionary, emptyDictionary);

		verifyAll();

		MetadataInformation metaInfo = informationCapture.getValue();
		assertEquals("Incorrect metadata size", 512, metaInfo.getMetadataSize());
	}

	@Test
	public void testAddHandshakeMetadataDownloadingMetadata() {
		Torrent torrent = createNiceMock(Torrent.class);
		Peer peerMock = createNiceMock(Peer.class);
		Bencoder bencoderMock = createMock(Bencoder.class);

		expect(peerMock.getTorrent()).andStubReturn(torrent);
		expect(torrent.isDownloadingMetadata()).andReturn(true);

		replayAll();

		UTMetadataExtension cut = new UTMetadataExtension();
		cut.addHandshakeMetadata(peerMock, bencoderMock);

		verifyAll();
	}

	@Test
	public void testAddHandshakeMetadata() {
		Torrent torrent = createNiceMock(Torrent.class);
		Peer peerMock = createNiceMock(Peer.class);
		Bencoder bencoderMock = createMock(Bencoder.class);
		MetadataFileSet metadataMock = createNiceMock(MetadataFileSet.class);

		expect(peerMock.getTorrent()).andStubReturn(torrent);
		expect(torrent.isDownloadingMetadata()).andReturn(false);
		expect(torrent.getMetadata()).andReturn(Optional.of(metadataMock));
		expect(metadataMock.getTotalFileSize()).andReturn(512L);
		bencoderMock.string("metadata_size");
		bencoderMock.integer(512);

		replayAll();

		UTMetadataExtension cut = new UTMetadataExtension();
		cut.addHandshakeMetadata(peerMock, bencoderMock);

		verifyAll();
	}

	@Test
	public void testGetMessageReject() {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(UTF8));

		UTMetadataExtension cut = new UTMetadataExtension();
		IMessage message = cut.getMessage(inStream);

		assertNotNull("A message should have been returned", message);
		assertTrue("Incorrect message type returned. Expected: MessageReject.", message instanceof MessageReject);
	}

	@Test
	public void testGetMessageRequest() {
		InStream inStream = new InStream("d8:msg_typei0e5:piecei5ee".getBytes(UTF8));

		UTMetadataExtension cut = new UTMetadataExtension();
		IMessage message = cut.getMessage(inStream);

		assertNotNull("A message should have been returned", message);
		assertTrue("Incorrect message type returned. Expected: MessageRequest.", message instanceof MessageRequest);
	}

	@Test
	public void testGetMessageUnknown() {
		InStream inStream = new InStream("d8:msg_typei151e5:piecei42ee".getBytes(UTF8));

		UTMetadataExtension cut = new UTMetadataExtension();
		IMessage message = cut.getMessage(inStream);

		assertNotNull("A message should have been returned", message);
		assertTrue("Incorrect message type returned. Expected: MessageUnknown.", message instanceof MessageUnknown);
	}

	@Test
	public void testGetMessageData() {
		InStream inStream = new InStream("d8:msg_typei1e5:piecei42ee".getBytes(UTF8));

		UTMetadataExtension cut = new UTMetadataExtension();
		IMessage message = cut.getMessage(inStream);

		assertNotNull("A message should have been returned", message);
		assertTrue("Incorrect message type returned. Expected: MessageData.", message instanceof MessageData);
	}

}