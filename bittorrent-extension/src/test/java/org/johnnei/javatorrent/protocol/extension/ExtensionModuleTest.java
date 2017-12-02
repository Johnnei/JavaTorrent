package org.johnnei.javatorrent.protocol.extension;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.extension.MessageHandshake;
import org.johnnei.javatorrent.test.StubExtension;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtensionModuleTest {

	@Test
	public void testConfigureTorrentClient() {
		TorrentClient.Builder builderMock = mock(TorrentClient.Builder.class);
		ArgumentCaptor<Supplier<IMessage>> supplierCapture = ArgumentCaptor.forClass(Supplier.class);

		when(builderMock.registerMessage(eq(20), supplierCapture.capture())).thenReturn(builderMock);
		when(builderMock.enableExtensionBit(eq(20))).thenReturn(builderMock);

		ExtensionModule cut = new ExtensionModule.Builder().build();
		cut.configureTorrentClient(builderMock);

		assertTrue(supplierCapture.getValue().get() instanceof MessageExtension, "Extended message supplier did not produce correct class");
	}

	@Test
	public void testSimpleMethods() {
		ExtensionModule cut = new ExtensionModule.Builder().build();

		// These two methods don't do anything so shouldn't cause an exception when nothing gets registered.
		cut.onBuild(null);
		cut.onShutdown();

		assertEquals(10, cut.getRelatedBep(), "Incorrect related BEP");
		assertEquals(0, cut.getDependsOn().size(), "No other BEPs required");
		assertNotNull(cut.createHandshakeMessage(), "Message cannot be null");
		assertTrue(cut.createHandshakeMessage() instanceof MessageHandshake, "Incorrect class type");
	}

	@Test
	public void testBuilderRegisterExtension() {
		StubExtension extensionOne = new StubExtension("ut_metadata");
		StubExtension extensionTwo = new StubExtension("ut_metadata");
		assertThrows(IllegalStateException.class, () -> new ExtensionModule.Builder()
				.registerExtension(extensionOne)
				.registerExtension(extensionTwo)
				.build());
	}

	@Test
	public void testGetExtensionById() {
		final String utMetadata = "ut_metadata";
		final String jtTest = "jt_test";

		StubExtension extensionOne = new StubExtension(utMetadata);
		StubExtension extensionTwo = new StubExtension(jtTest);
		ExtensionModule cut = new ExtensionModule.Builder()
				.registerExtension(extensionOne)
				.registerExtension(extensionTwo)
				.build();

		assertEquals(utMetadata, cut.getExtensionById(1).get().getExtensionName(), "Extension ID 1 was expected to be ut_metadata");
		assertEquals(extensionOne, cut.getExtensionByName(utMetadata).get(), "Extension ut_metadata was expected to be found");
		assertEquals(jtTest, cut.getExtensionById(2).get().getExtensionName(), "Extension ID 2 was expected to be jt_test");
		assertEquals(extensionTwo, cut.getExtensionByName(jtTest).get(), "Extension jt_test was expected to be found");
		assertEquals(Optional.empty(), cut.getExtensionById(3), "Extension ID 3 was expected to be empty");
		assertNotPresent("No extension with the given name registered", cut.getExtensionByName("jt_nope"));
	}

	@Test
	public void testOnPostHandshakeNoExtension() throws Exception {
		Peer peerMock = mock(Peer.class);

		when(peerMock.hasExtension(eq(5), eq(0x10))).thenReturn(false);

		ExtensionModule cut = new ExtensionModule.Builder().build();

		cut.onPostHandshake(peerMock);
	}

	@Test
	public void testOnPostHandshakeWithExtension() throws Exception {
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Peer peerMock = mock(Peer.class);

		when(peerMock.hasExtension(eq(5), eq(0x10))).thenReturn(true);
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);

		ExtensionModule cut = new ExtensionModule.Builder().build();

		cut.onPostHandshake(peerMock);

		verify(peerMock).addModuleInfo(isA(PeerExtensions.class));
		verify(socketMock).enqueueMessage(isA(MessageExtension.class));
	}

}
