package org.johnnei.javatorrent.protocol.extension;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.function.Supplier;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.extension.MessageHandshake;
import org.johnnei.javatorrent.test.StubExtension;
import org.johnnei.javatorrent.torrent.download.peer.Extensions;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class ExtensionModuleTest extends EasyMockSupport {

	@Test
	public void testConfigureTorrentClient() {
		TorrentClient.Builder builderMock = createMock(TorrentClient.Builder.class);
		Capture<Supplier<IMessage>> supplierCapture = newCapture();

		expect(builderMock.registerMessage(eq(20), and(notNull(), capture(supplierCapture)))).andReturn(builderMock);
		builderMock.enableExtensionBit(eq(20));

		replayAll();

		ExtensionModule cut = new ExtensionModule.Builder().build();
		cut.configureTorrentClient(builderMock);

		verifyAll();

		assertTrue("Extended message supplier did not produce correct class", supplierCapture.getValue().get() instanceof MessageExtension);
	}

	@Test
	public void testSimpleMethods() {
		ExtensionModule cut = new ExtensionModule.Builder().build();

		// These two methods don't do anything so shouldn't cause an exception when nothing gets registered.
		cut.onBuild(null);
		cut.onShutdown();

		assertEquals("Incorrect related BEP", 10, cut.getRelatedBep());
		assertEquals("No other BEPs required", 0, cut.getDependsOn().size());
		assertNotNull("Message cannot be null", cut.createHandshakeMessage());
		assertTrue("Incorrect class type", cut.createHandshakeMessage() instanceof MessageHandshake);
	}

	@Test(expected=IllegalStateException.class)
	public void testBuilderRegisterExtension() {
		StubExtension extensionOne = new StubExtension("ut_metadata");
		StubExtension extensionTwo = new StubExtension("ut_metadata");
		new ExtensionModule.Builder()
				.registerExtension(extensionOne)
				.registerExtension(extensionTwo)
				.build();
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

		assertEquals("Extension ID 1 was expected to be ut_metadata", utMetadata, cut.getExtensionById(1).get().getExtensionName());
		assertEquals("Extension ID 2 was expected to be jt_test", jtTest, cut.getExtensionById(2).get().getExtensionName());
		assertEquals("Extension ID 3 was expected to be empty", Optional.empty(), cut.getExtensionById(3));
	}

	@Test
	public void testOnPostHandshakeNoExtension() throws Exception {
		Extensions extensionMock = createMock(Extensions.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getExtensions()).andReturn(extensionMock).atLeastOnce();
		expect(extensionMock.hasExtension(eq(5), eq(0x10))).andReturn(false).atLeastOnce();

		replayAll();

		ExtensionModule cut = new ExtensionModule.Builder().build();

		cut.onPostHandshake(peerMock);

		verifyAll();
	}

	@Test
	public void testOnPostHandshakeWithExtension() throws Exception {
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		Extensions extensionMock = createMock(Extensions.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getExtensions()).andReturn(extensionMock).atLeastOnce();
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock).atLeastOnce();
		expect(extensionMock.hasExtension(eq(5), eq(0x10))).andReturn(true).atLeastOnce();
		socketMock.queueMessage(and(notNull(), isA(MessageExtension.class)));

		replayAll();

		ExtensionModule cut = new ExtensionModule.Builder().build();

		cut.onPostHandshake(peerMock);

		verifyAll();
	}

}
