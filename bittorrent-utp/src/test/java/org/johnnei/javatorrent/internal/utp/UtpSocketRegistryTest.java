package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtpSocketRegistryTest {

	private UtpSocketRegistry cut;

	private DatagramChannel channelMock;

	@BeforeEach
	public void setUp() {
		channelMock = mock(DatagramChannel.class);
		cut = new UtpSocketRegistry(channelMock);
	}

	private UtpSocket createSocket(int connectionId) throws IOException {
		SocketAddress address = mock(SocketAddress.class);
		UtpPacket synPacket = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);

		when(synPacket.getHeader()).thenReturn(header);
		when(header.getConnectionId()).thenReturn((short) connectionId);

		return cut.createSocket(address, synPacket);
	}

	@Test
	public void testGetSocketCreateWhenNotExist() throws Exception {
		assertThrows(UtpProtocolViolationException.class, () -> cut.getSocket((short) 5));
	}

	@Test
	public void testGetSocketCreateWhenDuplicate() throws Exception {
		createSocket(5);
		assertThrows(UtpProtocolViolationException.class, () -> createSocket(5));
	}

	@Test
	public void testCreateSocket() throws Exception {
		SocketAddress address = mock(SocketAddress.class);
		UtpPacket synPacket = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);

		when(synPacket.getHeader()).thenReturn(header);

		UtpSocket socket = cut.createSocket(address, synPacket);
		assertThat("A socket should have been registered.", socket, notNullValue());
	}

	@Test
	public void testGetSocketAfterCreation() throws Exception {
		UtpSocket socket = createSocket(5);
		// The socket is created on the RECEIVE id, the SYN packet contains the id on which we will SEND.
		UtpSocket socketTwo = cut.getSocket((short) 6);

		assertThat("A socket must be reused when the same connection id is used.", socketTwo, sameInstance(socket));
		assertThat("All sockets must be returned", cut.getAllSockets(), hasSize(1));
	}

	@Test
	public void testGetSocketDoNotReuseSocketOnDifferentId() throws Exception {
		UtpSocket socket = createSocket(5);
		UtpSocket socketTwo = createSocket(7);

		assertThat("A socket must be reused when the same connection id is used.", socketTwo, not(sameInstance(socket)));
		assertThat("All sockets must be returned", cut.getAllSockets(), hasSize(2));
	}

	@Test
	public void testRemoveShutdownSockets() {
		UtpSocket socket1 = mock(UtpSocket.class);
		when(socket1.isShutdown()).thenReturn(false);

		UtpSocket socket2 = mock(UtpSocket.class);
		when(socket2.isShutdown()).thenReturn(true);

		cut.allocateSocket((id) -> socket1);
		cut.allocateSocket((id) -> socket2);

		assertThat("Allocate failure", cut.getAllSockets(), hasSize(2));
		cut.removeShutdownSockets();
		assertThat("Socket 2 should have been removed.", cut.getAllSockets(), hasSize(1));
	}

}
