package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.internal.utils.CheckedSupplier;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpSocketRegistryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@InjectMocks
	private UtpSocketRegistry cut;

	@Mock
	private CheckedSupplier<DatagramChannel, IOException> channelSupplier;

	private DatagramChannel channelMock;

	@Before
	public void setUp() throws Exception {
		Whitebox.setInternalState(cut, "channelSupplier", channelSupplier);
		channelMock = mock(DatagramChannel.class);
		when(channelSupplier.get()).thenReturn(channelMock);
	}

	private UtpSocket createSocket(int connectionId) throws IOException {
		SocketAddress address = mock(SocketAddress.class);
		UtpPacket synPacket = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);

		when(synPacket.getHeader()).thenReturn(header);
		when(header.getConnectionId()).thenReturn((short) connectionId);

		UtpSocket socket = cut.createSocket(address, synPacket);
		verify(channelMock).connect(same(address));
		return socket;
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
		UtpSocket socketTwo = cut.getSocket((short) 5);

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
