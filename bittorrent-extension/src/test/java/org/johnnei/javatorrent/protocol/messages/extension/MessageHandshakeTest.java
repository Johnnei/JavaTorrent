package org.johnnei.javatorrent.protocol.messages.extension;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class MessageHandshakeTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		IExtension extensionMock = createMock(IExtension.class);
		Peer peerMock = createMock(Peer.class);

		expect(extensionMock.getExtensionName()).andReturn("jt_mock").atLeastOnce();
		extensionMock.addHandshakeMetadata(same(peerMock), notNull());

		replayAll();

		MessageHandshake cut = new MessageHandshake(peerMock, Collections.singletonMap(1, extensionMock));
		OutStream outStream = new OutStream();
		cut.write(outStream);

		verifyAll();

		final String expectedOutput = String.format("d1:md7:jt_mocki1ee1:v%d:%se", Version.BUILD.length(), Version.BUILD);
		assertEquals("Incorrect generated bencoding", expectedOutput, new String(outStream.toByteArray(), Charset.forName("UTF-8")));
		assertEquals("Incorrect lenght", expectedOutput.length(), cut.getLength());
	}

	@Test
	public void testProcess() {
		Peer peerMock = createMock(Peer.class);
		IExtension extensionMock = createMock(IExtension.class);
		PeerExtensions peerExtensionsMock = createMock(PeerExtensions.class);

		expect(peerMock.getModuleInfo(eq(PeerExtensions.class))).andStubReturn(Optional.of(peerExtensionsMock));
		peerMock.setClientName(eq("JavaTorrent 0.05.0"));
		peerMock.setAbsoluteRequestLimit(eq(250));
		extensionMock.processHandshakeMetadata(same(peerMock), notNull(), notNull());

		String input = "d1:md7:jt_mocki1ee1:v18:JavaTorrent 0.05.04:reqqi250ee";

		expect(extensionMock.getExtensionName()).andStubReturn("jt_mock");
		peerExtensionsMock.registerExtension(eq(1), eq("jt_mock"));

		replayAll();
		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testProcessModuleNotRegistered() {
		Peer peerMock = createMock(Peer.class);
		IExtension extensionMock = createMock(IExtension.class);

		expect(peerMock.getModuleInfo(eq(PeerExtensions.class))).andStubReturn(Optional.empty());
		peerMock.setClientName(eq("JavaTorrent 0.05.0"));
		peerMock.setAbsoluteRequestLimit(eq(250));

		String input = "d1:md7:jt_mocki1ee1:v18:JavaTorrent 0.05.04:reqqi250ee";

		expect(extensionMock.getExtensionName()).andStubReturn("jt_mock");

		replayAll();
		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testProcessNoInfo() {
		Peer peerMock = createMock(Peer.class);
		IExtension extensionMock = createMock(IExtension.class);

		String input = "de";

		replayAll();
		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testProcessCorruptedInfo() {
		Peer peerMock = createMock(Peer.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		IExtension extensionMock = createMock(IExtension.class);

		String input = "d1:mj3ee";

		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		socketMock.close();

		replayAll();
		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testSimpleMethods() {
		MessageHandshake cut = new MessageHandshake(Collections.emptyList());

		cut.setReadDuration(5);
		assertEquals("Incorrect packet ID", 0, cut.getId());
		assertTrue("Incorrect start of toString", cut.toString().startsWith("MessageHandshake["));
	}

}
