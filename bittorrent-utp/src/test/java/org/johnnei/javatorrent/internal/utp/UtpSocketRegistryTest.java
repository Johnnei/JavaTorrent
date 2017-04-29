package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.internal.utils.CheckedSupplier;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpSocketRegistryTest {

	@InjectMocks
	private UtpSocketRegistry cut;

	@Mock
	private CheckedSupplier<DatagramChannel, IOException> channelSupplier;

	@Before
	public void setUp() throws Exception {
		Whitebox.setInternalState(cut, "channelSupplier", channelSupplier);
		when(channelSupplier.get()).thenReturn(mock(DatagramChannel.class));
	}

	@Test
	public void testGetSocketCreateWhenNotExist() throws Exception {
		InetSocketAddress socketAddress = mock(InetSocketAddress.class);
		UtpSocket socket = cut.getSocket(socketAddress, (short) 5);

		assertThat("A socket must be created if not existing", socket, notNullValue());
	}

	@Test
	public void testGetSocketReuseSocket() throws Exception {
		InetSocketAddress socketAddress = mock(InetSocketAddress.class);
		UtpSocket socket = cut.getSocket(socketAddress, (short) 5);
		UtpSocket socketTwo = cut.getSocket(socketAddress, (short) 5);

		assertThat("A socket must be reused when the same connection id is used.", socketTwo, sameInstance(socket));
	}

	@Test
	public void testGetSocketDoNotReuseSocketOnDifferentId() throws Exception {
		InetSocketAddress socketAddress = mock(InetSocketAddress.class);
		UtpSocket socket = cut.getSocket(socketAddress, (short) 5);
		UtpSocket socketTwo = cut.getSocket(socketAddress, (short) 7);

		assertThat("A socket must be reused when the same connection id is used.", socketTwo, not(sameInstance(socket)));
	}

}
