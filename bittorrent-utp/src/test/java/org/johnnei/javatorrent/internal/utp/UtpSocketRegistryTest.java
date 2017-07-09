package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpSocketRegistryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@InjectMocks
	private UtpSocketRegistry cut;

	@Mock
	private DatagramChannel channelMock;

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
		thrown.expect(UtpProtocolViolationException.class);
		cut.getSocket((short) 5);
	}

	@Test
	public void testGetSocketCreateWhenDuplicate() throws Exception {
		thrown.expect(UtpProtocolViolationException.class);
		createSocket(5);
		createSocket(5);
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
		assertThat("All sockets must be returned", cut.getAllSockets(), IsCollectionWithSize.hasSize(1));
	}

	@Test
	public void testGetSocketDoNotReuseSocketOnDifferentId() throws Exception {
		UtpSocket socket = createSocket(5);
		UtpSocket socketTwo = createSocket(7);

		assertThat("A socket must be reused when the same connection id is used.", socketTwo, not(sameInstance(socket)));
		assertThat("All sockets must be returned", cut.getAllSockets(), IsCollectionWithSize.hasSize(2));
	}

}
