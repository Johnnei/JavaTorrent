package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.DataPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.FinPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.ResetPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UtpSocketTest {

	@Rule
	public Timeout timeout = Timeout.seconds(30);

	private Exception threadException;

	private DatagramChannel channel;

	@Before
	public void setUp() throws Exception {
		channel = mock(DatagramChannel.class);
		when(channel.send(any(ByteBuffer.class), any())).thenAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			int sent = buffer.remaining();
			buffer.get(new byte[sent]);
			return sent;
		});
	}

	@Test
	public void testAcceptRemoteConnection() throws IOException {
		UtpPacket synPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 0)
				.setConnectionId((short) 5)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 1)
				.setType(PacketType.SYN.getTypeField())
				.build(),
			new SynPayload()
		);

		UtpSocket socket = UtpSocket.createRemoteConnecting(channel, synPacket);
		socket.onReceivedPacket(synPacket);

		socket.processSendQueue();

		ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channel).send(bufferArgumentCaptor.capture(), any());
		ByteBuffer buffer = bufferArgumentCaptor.getValue();

		assertThat("Packet would arrive at wrong socket.", buffer.getShort(2), equalTo((short) 5));
		assertThat("Response to ST_SYN should be ST_STATE.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.STATE.getTypeField()));
		assertThat("SYN packet should receive an ACK.", buffer.getShort(18), equalTo((short) 1));
		assertThat("Connection state should have transitioned to CS_SYN_RECV", socket.getConnectionState(), is(ConnectionState.SYN_RECEIVED));

		// At this point the initiating endpoint has transition to CS_CONNECTED and should start sending bytes.

		short ackNumber = buffer.getShort(8);
		UtpPacket dataPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber(ackNumber)
				.setConnectionId((short) 5)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 2)
				.setType(PacketType.DATA.getTypeField())
				.build(),
			new DataPayload(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5}))
		);

		socket.onReceivedPacket(dataPacket);

		// At this point the accepting endpoint should have transitioned to CS_CONNECTED.
		assertThat("Connection should be considered as OK after first ST_DATA packet", socket.getConnectionState(), is(ConnectionState.CONNECTED));
	}

	@Test
	public void testInitiateConnection() throws Exception {
		UtpSocket socket = UtpSocket.createInitiatingSocket(channel, (short) 42);
		final InetSocketAddress socketAddress = mock(InetSocketAddress.class);

		Thread connector = new Thread(() -> {
			try {
				socket.connect(socketAddress);
			} catch (IOException e) {
				threadException = e;
			}
		});

		connector.start();

		await("Socket must send out SYN packet after calling connect.").until(() -> socket.getConnectionState() == ConnectionState.SYN_SENT);

		socket.processSendQueue();

		ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channel).send(bufferArgumentCaptor.capture(), same(socketAddress));
		ByteBuffer buffer = bufferArgumentCaptor.getValue();

		assertThat("Packet would indicate wrong return path.", buffer.getShort(2), equalTo((short) 42));
		assertThat("Response to ST_SYN should be ST_STATE.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.SYN.getTypeField()));
		assertThat("Packet Sequence number must be 1", buffer.getShort(16), equalTo((short) 1));
		assertThat("Initial timestamp_diff is unknown.", buffer.getInt(8), equalTo(0));

		assertThat("The connect call should be blocking until either connection has been made or timeout", connector.isAlive(), is(true));

		// Respond to the SYN with ST to confirm the connection.
		UtpPacket stPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 1)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 675)
				.setType(PacketType.STATE.getTypeField())
				.build(),
			new StatePayload()
		);

		socket.onReceivedPacket(stPacket);

		assertThat("Connection should be considered as OK after receiving ACK on ST_SYN.", socket.getConnectionState(), is(ConnectionState.CONNECTED));

		connector.join(TimeUnit.SECONDS.toMillis(5));

		if (threadException != null) {
			throw threadException;
		}
	}

	private UtpSocket prepareSocketAfterHandshake() throws Exception {
		UtpSocket socket = UtpSocket.createInitiatingSocket(channel, (short) 42);

		Thread connector = new Thread(() -> {
			try {
				socket.connect(mock(InetSocketAddress.class));
			} catch (IOException e) {
				threadException = e;
			}
		});

		connector.start();

		await("Socket must send out SYN packet after calling connect.").until(() -> socket.getConnectionState() == ConnectionState.SYN_SENT);

		socket.processSendQueue();

		// Respond to the SYN with ST to confirm the connection.
		UtpPacket stPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 1)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 675)
				.setType(PacketType.STATE.getTypeField())
				.build(),
			new StatePayload()
		);

		socket.onReceivedPacket(stPacket);

		connector.join(TimeUnit.SECONDS.toMillis(5));

		if (threadException != null) {
			throw new IllegalStateException("Failed to prepare socket", threadException);
		}

		return socket;
	}

	@Test
	public void testSend() throws Exception {
		UtpSocket socket = prepareSocketAfterHandshake();

		socket.send(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));

		socket.processSendQueue();

		ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channel, times(2)).send(bufferArgumentCaptor.capture(), any(InetSocketAddress.class));
		ByteBuffer buffer = bufferArgumentCaptor.getAllValues().get(1);

		// Validate header
		assertThat("Connection id for second packet should be 1 higher than the one used in SYN packet.", buffer.getShort(2), equalTo((short) 43));
		assertThat("Data packet should have been sent.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.DATA.getTypeField()));
		assertThat("timestamp_diff should have been updated.", buffer.getInt(8), not(equalTo(0)));
		assertThat("Packet Sequence number must increment", buffer.getShort(16), equalTo((short) 2));
		assertThat("Acknowledge field should contain sequence number of the ST_STATE confirming the connection.", buffer.getShort(18), equalTo((short) 675));
		// Validate payload
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(20), equalTo((byte) 1));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(21), equalTo((byte) 2));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(22), equalTo((byte) 3));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(23), equalTo((byte) 4));
	}

	@Test
	public void testReset() throws Exception {
		UtpSocket socket = prepareSocketAfterHandshake();

		socket.send(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));

		socket.processSendQueue();

		UtpPacket resetPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 1)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 675)
				.setType(PacketType.RESET.getTypeField())
				.build(),
			new ResetPayload()
		);

		// Make the packet considered lost.
		assertThat(socket.isShutdown(), is(false));
		socket.onReceivedPacket(resetPacket);
		assertThat("Reset packet should can abrupt closing.", socket.isShutdown(), is(true));
		assertThat("Reset packet should can abrupt closing.", socket.isClosed(), is(true));
	}

	@Test
	public void testPacketLoss() throws Exception {
		UtpSocket socket = prepareSocketAfterHandshake();

		socket.send(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));

		socket.processSendQueue();

		UtpPacket stPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 1)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 675)
				.setType(PacketType.STATE.getTypeField())
				.build(),
			new StatePayload()
		);

		// Make the packet considered lost.
		socket.onReceivedPacket(stPacket);
		socket.onReceivedPacket(stPacket);
		socket.onReceivedPacket(stPacket);

		socket.processSendQueue();

		ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channel, times(3)).send(bufferArgumentCaptor.capture(), any(InetSocketAddress.class));

		// This should be packet[seq_nr=2] again
		ByteBuffer buffer = bufferArgumentCaptor.getAllValues().get(2);

		// Validate header
		assertThat("Connection id for second packet should be 1 higher than the one used in SYN packet.", buffer.getShort(2), equalTo((short) 43));
		assertThat("Data packet should have been sent.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.DATA.getTypeField()));
		assertThat("Packet Sequence number must increment", buffer.getShort(16), equalTo((short) 2));
		assertThat("Acknowledge field should contain sequence number of the ST_STATE confirming the connection.", buffer.getShort(18), equalTo((short) 675));
		// Validate payload
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(20), equalTo((byte) 1));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(21), equalTo((byte) 2));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(22), equalTo((byte) 3));
		assertThat("Payload should have been append to the end of the packet header.", buffer.get(23), equalTo((byte) 4));
	}

	@Test
	public void testReceive() throws Exception {
		UtpSocket socket = prepareSocketAfterHandshake();

		UtpPacket dataPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 2)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 676)
				.setType(PacketType.DATA.getTypeField())
				.build(),
			new DataPayload(ByteBuffer.wrap(new byte[]{ 1, 2, 3, 4 }))
		);

		InputStream inputStream = socket.getInputStream();

		socket.onReceivedPacket(dataPacket);

		byte[] data = new byte[4];
		assertThat(inputStream.read(data), equalTo(4));
		assertThat(data, equalTo(new byte[] { 1, 2, 3, 4 }));
	}

	@Test
	public void testShutdownInputOutOfOrder() throws Exception {
		UtpSocket socket = prepareSocketAfterHandshake();

		UtpPacket dataPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 2)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 676)
				.setType(PacketType.DATA.getTypeField())
				.build(),
			new DataPayload(ByteBuffer.wrap(new byte[]{ 1, 2, 3, 4 }))
		);

		UtpPacket finPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 2)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 677)
				.setType(PacketType.FIN.getTypeField())
				.build(),
			new FinPayload()
		);

		// Shutdown input
		socket.onReceivedPacket(finPacket);
		assertThat("Output should be shutdown after receiving FIN", socket.isOutputShutdown(), is(true));

		// Should trigger shutdown of output
		socket.processSendQueue();
		assertThat("Data packet is missing, can't be shutdown", socket.isShutdown(), is(false));

		socket.onReceivedPacket(dataPacket);
		assertThat("All packets are received and sent, socket can be removed.", socket.isShutdown(), is(true));
	}

	@Test
	public void testShutdownOutputOutOfOrder() throws Exception {
		ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

		UtpSocket socket = prepareSocketAfterHandshake();

		socket.send(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));

		UtpPacket finPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 2)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 676)
				.setType(PacketType.FIN.getTypeField())
				.build(),
			new FinPayload()
		);

		// Shutdown input
		socket.onReceivedPacket(finPacket);
		assertThat("Output should be shutdown after receiving FIN", socket.isOutputShutdown(), is(true));

		// Should trigger sending of DATA before shutting down the output.
		socket.processSendQueue();
		assertThat("Data packet has not been sent, can't be shutdown", socket.isShutdown(), is(false));

		// Should trigger the sending of FIN.
		socket.processSendQueue();

		verify(channel, times(3)).send(bufferArgumentCaptor.capture(), any(InetSocketAddress.class));

		// Validate packet 2 was DATA
		ByteBuffer buffer = bufferArgumentCaptor.getAllValues().get(1);
		assertThat("Data packet should have been sent.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.DATA.getTypeField()));

		// Validate packet 3 was FIN
		buffer = bufferArgumentCaptor.getAllValues().get(2);
		assertThat("FIN packet should have been sent.", (byte) (buffer.get(0) >>> 4), equalTo(PacketType.FIN.getTypeField()));

		// Validate socket is not in shutdown state.
		assertThat("Sent DATA packet has not been acked yet, socket can't be removed.", socket.isShutdown(), is(false));

		UtpPacket stPacket = new UtpPacket(
			new UtpHeader.Builder()
				.setAcknowledgeNumber((short) 2)
				.setConnectionId((short) 42)
				.setExtension((byte) 0)
				.setSequenceNumber((short) 676)
				.setType(PacketType.STATE.getTypeField())
				.build(),
			new StatePayload()
		);

		socket.onReceivedPacket(stPacket);
		assertThat("All packets are received and sent, socket can be removed.", socket.isShutdown(), is(true));
	}

}
