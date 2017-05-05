package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UtpSocketTest {

	@Test
	public void testAcceptRemoteConnection() throws IOException {
		DatagramChannel channel = mock(DatagramChannel.class);

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
		verify(channel).write(bufferArgumentCaptor.capture());
		ByteBuffer buffer = bufferArgumentCaptor.getValue();

		assertThat("Packet would arrive at wrong socket.", buffer.getShort(1), equalTo((short) 5));
		assertThat("Response to ST_SYN should be ST_STATE.", buffer.getShort(0) >>> 4, equalTo(PacketType.STATE.getTypeField()));
		assertThat("SYN packet should receive an ACK.", buffer.getShort(9), equalTo((short) 1));
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
			mock(Payload.class)
		);

		socket.onReceivedPacket(dataPacket);

		// At this point the accepting endpoint should have transitioned to CS_CONNECTED.
		assertThat("Connection should be considered as OK after first ST_DATA packet", socket.getConnectionState(), is(ConnectionState.CONNECTED));
	}

}
