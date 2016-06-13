package org.johnnei.javatorrent.internal.network.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.internal.utp.UtpTimeout;
import org.johnnei.javatorrent.internal.utp.UtpWindow;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpAckHandler;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;
import org.johnnei.javatorrent.test.DummyEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.isA;
import static org.johnnei.javatorrent.test.DummyEntity.createRandomBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpSocketImpl}
 */
public class UtpSocketImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketImpl.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Exception threadException;

	private UtpMultiplexer multiplexerMock;

	private UtpSocketImpl cut;

	@Before
	public void setUp() {
		multiplexerMock = mock(UtpMultiplexer.class);
		cut = new UtpSocketImpl.Builder()
				.setUtpMultiplexer(multiplexerMock)
				.build();
	}

	@Test
	public void testCreateConnectingSocket() {
		assertEquals("Incorrect connection id's", cut.getReceivingConnectionId() + 1, cut.getSendingConnectionId());
		assertEquals("Incorrect sequence number", 1, cut.nextSequenceNumber());
		assertEquals("Incorrect connection state", ConnectionState.CONNECTING, cut.getConnectionState());
	}

	@Test
	public void testCreateAcceptingSocket() {
		cut = new UtpSocketImpl.Builder()
				.setUtpMultiplexer(multiplexerMock)
				.setSocketAddress(new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()))
				.build((short) 5);

		assertEquals("Incorrect receiving connection id", 6, cut.getReceivingConnectionId());
		assertEquals("Incorrect sending connection id", 5, cut.getSendingConnectionId());
		assertEquals("Incorrect connection state", ConnectionState.CONNECTING, cut.getConnectionState());
	}

	@Test
	public void testClose() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);
		cut.bindIoStreams((short) 5);

		assertFalse("Output stream must not be closed before sending FIN packet.", cut.isOutputShutdown());
		assertFalse("Input stream should not be closed before sending FIN packet.", cut.isOutputShutdown());

		cut.close();

		ArgumentCaptor<DatagramPacket> packetCapture = ArgumentCaptor.forClass(DatagramPacket.class);
		verify(multiplexerMock).send(packetCapture.capture());

		assertEquals("Socket must sent FIN packet on close.", UtpProtocol.ST_FIN, (packetCapture.getValue().getData()[0] & 0xF0) >> 4);
		assertTrue("Output stream must be closed after sending a FIN packet.", cut.isOutputShutdown());
		assertFalse("Input stream must not be affected by sending a FIN packet.", cut.isInputShutdown());
	}

	@Test
	public void testHandleTimeout() {
		UtpWindow windowMock = mock(UtpWindow.class);
		Whitebox.setInternalState(cut, UtpWindow.class, windowMock);

		// Timeout should not be triggered here.
		cut.handleTimeout();

		verify(windowMock, never()).onTimeout();

		// Timeout must be triggered here.
		Clock shiftedClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1));
		Whitebox.setInternalState(cut, Clock.class, shiftedClock);

		cut.handleTimeout();

		verify(windowMock).onTimeout();
	}

	@Test
	public void testUpdateWindowTimeoutAndPacketSize() throws Exception {
		UtpTimeout timeoutMock = mock(UtpTimeout.class);
		UtpWindow windowMock = mock(UtpWindow.class);
		UtpAckHandler handlerMock = mock(UtpAckHandler.class);

		// Create a packet which is going to be ack'ed.
		UtpPacket packetMock = mock(UtpPacket.class);
		when(packetMock.getSequenceNumber()).thenReturn((short) 27);

		// Create a packet that will ack.
		UtpPacket statePacket = mock(UtpPacket.class);
		when(statePacket.getAcknowledgeNumber()).thenReturn((short) 27);
		when(statePacket.getTimestampDifferenceMicroseconds()).thenReturn(52);

		// Inject a socket address to allow sending of packets the multiplexer mock.
		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		when(handlerMock.onReceivedPacket(eq(statePacket))).thenReturn(Optional.of(packetMock));

		// Inject the packet to be ack'ed.
		Whitebox.setInternalState(cut, UtpAckHandler.class, handlerMock);
		Whitebox.setInternalState(cut, UtpTimeout.class, timeoutMock);
		Whitebox.setInternalState(cut, UtpWindow.class, windowMock);

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);
		cut.bindIoStreams((short) 5);

		cut.process(statePacket);

		verify(windowMock).update(statePacket);
		verify(timeoutMock).update(anyInt(), same(packetMock));
	}

	@Test
	public void testEndOfStreamSequenceNumber() {
		cut.setEndOfStreamSequenceNumber((short) 42);
		assertEquals("Incorrect End of Stream sequence number", 42, cut.getEndOfStreamSequenceNumber());
	}

	@Test
	public void testOnReset() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);

		cut.onReset();

		assertEquals("Connection should be closed after reset", ConnectionState.CLOSED, cut.getConnectionState());
	}

	@Test
	public void testOnResetWakeUpPendingWrites() throws Exception {
		thrown.expect(ExecutionException.class);
		thrown.expectCause(isA(IOException.class));

		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		UtpWindow windowMock = mock(UtpWindow.class);
		Whitebox.setInternalState(cut, UtpWindow.class, windowMock);

		when(windowMock.getSize()).thenReturn(5);

		// Get the locks to make the test reliable.
		ReentrantLock cutLock = Whitebox.getInternalState(cut, "notifyLock");
		Condition cutCondition = Whitebox.getInternalState(cut, "onPacketAcknowledged");

		FutureTask<Boolean> future = new FutureTask<>(() -> {
			cut.getOutputStream().write(createRandomBytes(150));
			return false;
		});

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);
		cut.bindIoStreams((short) 0);

		// Start the blocking write
		Thread thread = new Thread(future);
		thread.start();

		// Wait until the thread is blocking for the write action.
		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			cutLock.lock();
			try {
				cutLock.hasWaiters(cutCondition);
			} finally {
				cutLock.unlock();
			}
		});

		// Reset the connection.
		cut.onReset();

		future.get(1, TimeUnit.SECONDS);
	}

	@Test
	public void testOnResetDisconnecting() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.DISCONNECTING);

		cut.onReset();

		assertEquals("Connection should be closed after reset", ConnectionState.CLOSED, cut.getConnectionState());
	}

	@Test
	public void testNoAckOnStatePacket() throws Exception {
		UtpSocketImpl remoteSocket = mock(UtpSocketImpl.class);

		when(remoteSocket.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
		when(remoteSocket.nextSequenceNumber()).thenReturn((short) 5);
		when(remoteSocket.getAcknowledgeNumber()).thenReturn((short) 5);

		UtpPacket dataPacket = new UtpPacket(remoteSocket, new StatePayload());

		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);
		cut.bindIoStreams((short) 5);

		cut.process(dataPacket);

		verifyNoMoreInteractions(multiplexerMock);
	}

	@Test
	public void testConnect() throws Exception {
		Mockito.doAnswer(invocation -> {
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					LOGGER.info("Non-fatal interruption, test will take longer than usual", e);
				}

				UtpSocketImpl remoteSocket = mock(UtpSocketImpl.class);
				when(remoteSocket.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
				when(remoteSocket.nextSequenceNumber()).thenReturn((short) 5);

				UtpPacket responsePacket = new UtpPacket(remoteSocket, new StatePayload());

				try {
					cut.process(responsePacket);
				} catch (Exception e) {
					threadException = e;
				}
			});
			thread.setDaemon(true);
			thread.start();
			return null;
		}).when(multiplexerMock).send((DatagramPacket) notNull());

		cut.connect(new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()));

		if (threadException != null) {
			throw threadException;
		}

		assertEquals("Socket should have been connected", ConnectionState.CONNECTED, cut.getConnectionState());
		assertEquals("Packet should have been registered as acked", 5, cut.getAcknowledgeNumber());
	}

	@Test
	public void testConnectWhenInterrupted() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectMessage("Interruption");

		Mockito.doAnswer(invocation -> {
			Thread testRunnerThread = Thread.currentThread();
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					LOGGER.info("Non-fatal interruption, test will take longer than usual but will still fail.", e);
				}

				testRunnerThread.interrupt();

			});
			thread.setDaemon(true);
			thread.start();
			return null;
		}).when(multiplexerMock).send((DatagramPacket) notNull());

		cut.connect(new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()));

	}

	@Test
	public void testConnectWhenNoResponse() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectMessage("did not respond");

		// Get the locks to fake that we reached the timeout
		Lock lock = Whitebox.getInternalState(cut, "notifyLock");
		Condition wakeUpCondition = Whitebox.getInternalState(cut, "onPacketAcknowledged");

		Mockito.doAnswer(invocation -> {
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					LOGGER.info("Non-fatal interruption, test will take longer than usual", e);
				}
				lock.lock();
				try {
					wakeUpCondition.signalAll();
				} finally {
					lock.unlock();
				}
			});
			thread.setDaemon(true);
			thread.start();
			return null;
		}).when(multiplexerMock).send((DatagramPacket) notNull());

		cut.connect(new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()));

	}

}