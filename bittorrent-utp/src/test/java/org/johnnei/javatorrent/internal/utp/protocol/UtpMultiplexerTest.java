package org.johnnei.javatorrent.internal.utp.protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;

import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpMultiplexer}
 */
public class UtpMultiplexerTest {

	private UtpPayloadFactory payloadFactoryMock;

	private TorrentClient torrentClientMock;

	private DatagramSocket datagramSocketMock;

	private UtpSocketImpl.Builder utpSocketFactoryMock;

	private ScheduledExecutorService executorServiceMock;

	private UtpMultiplexer cut;

	private void injectMocks() throws ModuleBuildException {
		if (torrentClientMock == null) {
			torrentClientMock = mock(TorrentClient.class);
		}

		if (executorServiceMock == null) {
			executorServiceMock = mock(ScheduledExecutorService.class);
		}

		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(executorServiceMock);

		if (utpSocketFactoryMock == null) {
			utpSocketFactoryMock = mock(UtpSocketImpl.Builder.class);
		}

		cut = new UtpMultiplexerStub(torrentClientMock, datagramSocketMock);

		// Replace the actual implementation with the mocked, but assert that actual one is being created.
		assertNotNull("Missing original utpPayloadFactory", Whitebox.getInternalState(cut, UtpPayloadFactory.class));
		Whitebox.setInternalState(cut, "packetFactory", payloadFactoryMock);
		Whitebox.setInternalState(cut, "utpSocketFactory", utpSocketFactoryMock);
	}

	@Test
	public void testRegisterDuplicateSocket() throws Exception {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		when(socketMock.getReceivingConnectionId()).thenReturn((short) 5);

		UtpSocketImpl socketMockTwo = mock(UtpSocketImpl.class);
		when(socketMockTwo.getReceivingConnectionId()).thenReturn((short) 5);

		injectMocks();

		assertTrue("First call should have been accepted", cut.registerSocket(socketMock));
		assertFalse("Second call should have been declined", cut.registerSocket(socketMockTwo));
	}

	@Test
	public void testReceivePacket() throws Exception {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		when(socketMock.getReceivingConnectionId()).thenReturn((short) 5);

		datagramSocketMock = mock(DatagramSocket.class);
		datagramSocketMock.receive((DatagramPacket) notNull());
		Mockito.doAnswer(invocation -> {
			DatagramPacket p = (DatagramPacket) invocation.getArguments()[0];
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
			return null;
		}).when(datagramSocketMock).receive((DatagramPacket) notNull());

		payloadFactoryMock = mock(UtpPayloadFactory.class);
		IPayload payloadMock = mock(IPayload.class);
		when(payloadFactoryMock.createPayloadFromType(2)).thenReturn(payloadMock);

		injectMocks();

		cut.registerSocket(socketMock);
		cut.run();
	}

	@Test
	public void testReceivingConnection() throws Exception {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		utpSocketFactoryMock = mock(UtpSocketImpl.Builder.class);
		when(utpSocketFactoryMock.setSocketAddress((SocketAddress) notNull())).thenReturn(utpSocketFactoryMock);
		when(utpSocketFactoryMock.build(eq((short) 5))).thenReturn(socketMock);
		when(socketMock.getReceivingConnectionId()).thenReturn((short) 5);

		datagramSocketMock = mock(DatagramSocket.class);
		Mockito.doAnswer(invocation -> {
			DatagramPacket p = (DatagramPacket) invocation.getArguments()[0];
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
			return null;
		}).when(datagramSocketMock).receive((DatagramPacket) notNull());

		payloadFactoryMock = mock(UtpPayloadFactory.class);
		IPayload payloadMock = mock(IPayload.class);
		when(payloadFactoryMock.createPayloadFromType(2)).thenReturn(payloadMock);

		injectMocks();

		cut.run();
	}

	@Test
	public void testShutdown() throws Exception {
		datagramSocketMock = mock(DatagramSocket.class);
		datagramSocketMock.close();

		injectMocks();

		cut.shutdown();
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