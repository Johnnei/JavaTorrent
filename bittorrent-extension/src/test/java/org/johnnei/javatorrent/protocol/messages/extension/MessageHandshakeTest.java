package org.johnnei.javatorrent.protocol.messages.extension;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageHandshakeTest {

	@Test
	public void testWrite() {
		IExtension extensionMock = mock(IExtension.class);
		Peer peerMock = mock(Peer.class);

		when(extensionMock.getExtensionName()).thenReturn("jt_mock");

		MessageHandshake cut = new MessageHandshake(peerMock, Collections.singletonMap(1, extensionMock));
		OutStream outStream = new OutStream();
		cut.write(outStream);

		verify(extensionMock).addHandshakeMetadata(same(peerMock), notNull());

		final String whenedOutput = String.format("d1:md7:jt_mocki1ee1:v%d:%se", Version.BUILD.length(), Version.BUILD);
		assertEquals(whenedOutput, new String(outStream.toByteArray(), Charset.forName("UTF-8")), "Incorrect generated bencoding");
		assertEquals(whenedOutput.length(), cut.getLength(), "Incorrect lenght");
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);
		IExtension extensionMock = mock(IExtension.class);
		PeerExtensions peerExtensionsMock = mock(PeerExtensions.class);

		when(peerMock.getModuleInfo(eq(PeerExtensions.class))).thenReturn(Optional.of(peerExtensionsMock));

		String input = "d1:md7:jt_mocki1ee1:v18:JavaTorrent 0.05.04:reqqi250ee";

		when(extensionMock.getExtensionName()).thenReturn("jt_mock");

		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verify(peerMock).setClientName(eq("JavaTorrent 0.05.0"));
		verify(peerMock).setAbsoluteRequestLimit(eq(250));
		verify(extensionMock).processHandshakeMetadata(same(peerMock), notNull(), notNull());
		verify(peerExtensionsMock).registerExtension(eq(1), eq("jt_mock"));
	}

	@Test
	public void testProcessModuleNotRegistered() {
		Peer peerMock = mock(Peer.class);
		IExtension extensionMock = mock(IExtension.class);

		when(peerMock.getModuleInfo(eq(PeerExtensions.class))).thenReturn(Optional.empty());

		String input = "d1:md7:jt_mocki1ee1:v18:JavaTorrent 0.05.04:reqqi250ee";

		when(extensionMock.getExtensionName()).thenReturn("jt_mock");

		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verify(peerMock).setClientName(eq("JavaTorrent 0.05.0"));
		verify(peerMock).setAbsoluteRequestLimit(eq(250));
	}

	@Test
	public void testProcessNoInfo() {
		Peer peerMock = mock(Peer.class);
		IExtension extensionMock = mock(IExtension.class);

		String input = "de";

		when(extensionMock.getExtensionName()).thenReturn("jt_mock");

		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);
	}

	@Test
	public void testProcessCorruptedInfo() {
		Peer peerMock = mock(Peer.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		IExtension extensionMock = mock(IExtension.class);

		String input = "d1:mj3ee";

		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);

		MessageHandshake cut = new MessageHandshake(Collections.singletonList(extensionMock));
		cut.read(new InStream(input.getBytes(Charset.forName("UTF-8"))));

		cut.process(peerMock);

		verify(socketMock).close();
	}

	@Test
	public void testSimpleMethods() {
		MessageHandshake cut = new MessageHandshake(Collections.emptyList());

		assertEquals(0, cut.getId(), "Incorrect packet ID");
		assertTrue(cut.toString().startsWith("MessageHandshake["), "Incorrect start of toString");
	}

}
