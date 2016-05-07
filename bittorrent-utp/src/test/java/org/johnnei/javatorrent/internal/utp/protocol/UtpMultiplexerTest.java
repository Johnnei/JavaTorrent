package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;

import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link UtpMultiplexer}
 */
public class UtpMultiplexerTest extends EasyMockSupport {

	private UtpPayloadFactory payloadFactoryMock;

	private TorrentClient torrentClientMock;

	private DatagramSocket datagramSocketMock;

	private UtpSocketImpl.Builder utpSocketFactoryMock;

	private UtpMultiplexer cut;

	private void injectMockAndReplay() throws ModuleBuildException {
		if (torrentClientMock == null) {
			torrentClientMock = createMock(TorrentClient.class);
		}

		expect(torrentClientMock.getDownloadPort()).andReturn(27960);

		if (utpSocketFactoryMock == null) {
			utpSocketFactoryMock = createMock(UtpSocketImpl.Builder.class);
		}

		replayAll();

		cut = new UtpMultiplexerStub(torrentClientMock, datagramSocketMock);

		// Replace the actual implementation with the mocked, but assert that actual one is being created.
		assertNotNull("Missing original utpPayloadFactory", Whitebox.getInternalState(cut, UtpPayloadFactory.class));
		Whitebox.setInternalState(cut, "packetFactory", payloadFactoryMock);
		Whitebox.setInternalState(cut, "utpSocketFactory", utpSocketFactoryMock);
	}

	@Test
	public void testRegisterDuplicateSocket() throws Exception {
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		expect(socketMock.getReceivingConnectionId()).andReturn((short) 5).atLeastOnce();

		UtpSocketImpl socketMockTwo = createMock(UtpSocketImpl.class);
		expect(socketMockTwo.getReceivingConnectionId()).andReturn((short) 5).atLeastOnce();

		injectMockAndReplay();

		assertTrue("First call should have been accepted", cut.registerSocket(socketMock));
		assertFalse("Second call should have been declined", cut.registerSocket(socketMockTwo));
	}

	@Test
	public void testReceivePacket() throws Exception {
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		expect(socketMock.getReceivingConnectionId()).andReturn((short) 5).atLeastOnce();

		datagramSocketMock = createMock(DatagramSocket.class);
		datagramSocketMock.receive(notNull());
		expectLastCall().andDelegateTo(new DatagramSocket() {
			@Override
			public synchronized void receive(DatagramPacket p) throws IOException {
				byte[] data = new byte[] {
						// Type and version
						0x21,
						// Extension
						0x00,
						// Connection Id
						0x00, 0x05,
						// Timestamp
						0x12, 0x34, 0x56, 0x78,
						// Timestamp difference
						(byte) 0x87, 0x65, 0x43, 0x21,
						// Window size
						0x12, 0x34, 0x43, 0x21,
						// Sequence number
						0x43, 0x21,
						// Acknowledge number
						0x12, 0x21
				};
				p.setLength(data.length);
				TestUtils.copySection(data, p.getData(), 0);
			}
		});

		payloadFactoryMock = createMock(UtpPayloadFactory.class);
		IPayload payloadMock = createMock(IPayload.class);
		expect(payloadFactoryMock.createPayloadFromType(2)).andReturn(payloadMock);
		payloadMock.read(notNull());
		socketMock.process(notNull());

		injectMockAndReplay();

		cut.registerSocket(socketMock);
		cut.run();
	}

	@Test
	public void testReceivingConnection() throws Exception {
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		utpSocketFactoryMock = createMock(UtpSocketImpl.Builder.class);
		expect(utpSocketFactoryMock.setSocketAddress(notNull())).andReturn(utpSocketFactoryMock);
		expect(utpSocketFactoryMock.build(eq((short) 5))).andReturn(socketMock);
		expect(socketMock.getReceivingConnectionId()).andReturn((short) 5).atLeastOnce();

		datagramSocketMock = createMock(DatagramSocket.class);
		datagramSocketMock.receive(notNull());
		expectLastCall().andDelegateTo(new DatagramSocket() {
			@Override
			public synchronized void receive(DatagramPacket p) throws IOException {
				byte[] data = new byte[] {
						// Type and version
						0x21,
						// Extension
						0x00,
						// Connection Id
						0x00, 0x05,
						// Timestamp
						0x12, 0x34, 0x56, 0x78,
						// Timestamp difference
						(byte) 0x87, 0x65, 0x43, 0x21,
						// Window size
						0x12, 0x34, 0x43, 0x21,
						// Sequence number
						0x43, 0x21,
						// Acknowledge number
						0x12, 0x21
				};
				p.setLength(data.length);
				p.setPort(DummyEntity.findAvailableUdpPort());
				p.setAddress(InetAddress.getByName("localhost"));
				TestUtils.copySection(data, p.getData(), 0);
			}
		});

		payloadFactoryMock = createMock(UtpPayloadFactory.class);
		IPayload payloadMock = createMock(IPayload.class);
		expect(payloadFactoryMock.createPayloadFromType(2)).andReturn(payloadMock);
		payloadMock.read(notNull());
		socketMock.process(notNull());

		injectMockAndReplay();

		cut.run();
	}

	@Test
	public void testShutdown() throws Exception {
		datagramSocketMock = createMock(DatagramSocket.class);
		datagramSocketMock.close();

		injectMockAndReplay();

		cut.shutdown();

		verifyAll();
	}

	private static final class UtpMultiplexerStub extends UtpMultiplexer {

		UtpMultiplexerStub(TorrentClient torrentClient, DatagramSocket datagramSocket) throws ModuleBuildException {
			super(torrentClient);
			this.multiplexerSocket = datagramSocket;
		}

		@Override
		void startMultiplexer(int port) throws ModuleBuildException {
			// Don't bind to a socket.
		}
	}

}