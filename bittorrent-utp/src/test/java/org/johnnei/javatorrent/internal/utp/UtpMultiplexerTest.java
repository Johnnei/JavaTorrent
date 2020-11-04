package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.TorrentClientSettings;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketReader;
import org.johnnei.javatorrent.mock.MockDatagramChannel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class UtpMultiplexerTest {

	@Test
	@DisplayName("Discard ST_SYN when isAcceptingConnections = false")
	public void testRejectSynPacketNotAcceptingConnection() throws IOException {
		TorrentClient client = mock(TorrentClient.class);
		TorrentClientSettings settings = mock(TorrentClientSettings.class);
		PacketReader packetReader = mock(PacketReader.class);
		MockDatagramChannel channel = new MockDatagramChannel();
		UtpPeerConnectionAcceptor connectionAcceptor = mock(UtpPeerConnectionAcceptor.class);
		UtpHeader header = new UtpHeader.Builder()
			.setType(PacketType.SYN.getTypeField())
			.setExtension((byte) 0)
			.setConnectionId((short) 0)
			.setTimestamp(0)
			.setTimestampDifference(0)
			.setWindowSize(0)
			.setSequenceNumber((short) 0)
			.setAcknowledgeNumber((short) 0)
			.build();
		UtpPacket packet = new UtpPacket(header, new SynPayload());

		when(packetReader.read(any())).thenReturn(packet);
		when(channel.getMockitoMock().receive(any()))
			.thenReturn(InetSocketAddress.createUnresolved("localhost", 6881));

		when(client.getSettings()).thenReturn(settings);
		when(client.getExecutorService()).thenReturn(Executors.newSingleThreadScheduledExecutor());

		try (UtpMultiplexer multiplexer = new UtpMultiplexer.Builder(client, packetReader)
			.withChannelFactory(() -> channel)
			.withConnectionAcceptor(connectionAcceptor)
			.build()) {
			multiplexer.pollPackets();
		}

		verifyNoInteractions(connectionAcceptor);
	}

}
