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
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.internal.utp.UtpTimeout;
import org.johnnei.javatorrent.internal.utp.UtpWindow;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpAckHandler;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.test.DummyEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpSocketImpl}
 */
public class UtpSocketImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketImpl.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public Timeout timeout = Timeout.seconds(5);

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
		injectMultiplexerMock();

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
	public void testCloseOnClosedSocket() throws Exception {
		injectMultiplexerMock();

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CLOSED);
		cut.close();

		verify(multiplexerMock, never()).send(any());
	}

	@Test
	public void testHandleClose() throws IOException {
		// Inject a socket address to allow sending of packets the multiplexer mock.
		injectMultiplexerMock();

		cut.setConnectionState(ConnectionState.DISCONNECTING);

		cut.handleClose();

		verify(multiplexerMock).cleanUpSocket(same(cut));
		assertEquals("Socket should have been closed", ConnectionState.CLOSED, cut.getConnectionState());
		assertTrue("Output stream must be closed after socket has been cleaned up.", cut.isOutputShutdown());
	}

	@Test
	public void testHandleCloseNotClosing() throws IOException {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);
		injectMultiplexerMock();

		when(ackHandlerMock.hasPacketsInFlight()).thenReturn(true);
		cut.setConnectionState(ConnectionState.CONNECTED);

		cut.handleClose();

		verify(multiplexerMock, never()).cleanUpSocket(same(cut));
		assertEquals("Socket must not have been closed", ConnectionState.CONNECTED, cut.getConnectionState());
	}

	@Test
	public void testHandleCloseWaitingForPacket() throws IOException {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);
		injectMultiplexerMock();

		when(ackHandlerMock.hasPacketsInFlight()).thenReturn(false);
		cut.setConnectionState(ConnectionState.DISCONNECTING);
		cut.setEndOfStreamSequenceNumber((short) 1);

		cut.handleClose();

		verify(multiplexerMock, never()).cleanUpSocket(same(cut));
		assertEquals("Socket must not have been closed", ConnectionState.DISCONNECTING, cut.getConnectionState());
	}

	@Test
	public void testHandleCloseWaitingForAcks() throws IOException {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);
		injectMultiplexerMock();

		when(ackHandlerMock.hasPacketsInFlight()).thenReturn(true);
		cut.setConnectionState(ConnectionState.DISCONNECTING);

		cut.handleClose();

		verify(multiplexerMock, never()).cleanUpSocket(same(cut));
		assertEquals("Socket must not have been closed", ConnectionState.DISCONNECTING, cut.getConnectionState());
	}

	private void injectMultiplexerMock() {
		// Inject a socket address to allow sending of packets the multiplexer mock.
		InetSocketAddress socketAddress = new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort());
		Whitebox.setInternalState(cut, SocketAddress.class, socketAddress);
	}

	@Test
	public void testHandleTimeout() throws IOException {
		UtpWindow windowMock = mock(UtpWindow.class);
		Whitebox.setInternalState(cut, UtpWindow.class, windowMock);

		// Inject a socket address to allow sending of packets the multiplexer mock.
		injectMultiplexerMock();

		// Timeout should not be triggered here.
		cut.handleTimeout();

		verify(windowMock, never()).onTimeout();

		// Timeout must be triggered here.
		Clock shiftedClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1));
		Whitebox.setInternalState(cut, Clock.class, shiftedClock);

		cut.handleTimeout();

		verify(windowMock).onTimeout();
		verify(multiplexerMock).send(any());
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
		injectMultiplexerMock();

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
		injectMultiplexerMock();

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.CONNECTED);

		cut.onReset();

		assertEquals("Connection should be closed after reset", ConnectionState.CLOSED, cut.getConnectionState());
	}

	@Test
	public void testOnResetWakeUpPendingWrites() throws Exception {
		thrown.expect(ExecutionException.class);
		thrown.expectCause(isA(IOException.class));

		injectMultiplexerMock();

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
			hasWaiters(cutLock, cutCondition);
		});

		// Reset the connection.
		cut.onReset();

		future.get(1, TimeUnit.SECONDS);
	}

	@Test
	public void testOnResetDisconnecting() throws Exception {
		injectMultiplexerMock();

		// Force this connection to be connected
		cut.setConnectionState(ConnectionState.DISCONNECTING);

		cut.onReset();

		assertEquals("Connection should be closed after reset", ConnectionState.CLOSED, cut.getConnectionState());
	}

	@Test
	public void testConnectWhenInterrupted() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectMessage("Interruption");

		ReentrantLock lock = Whitebox.getInternalState(cut, "notifyLock");
		Condition wakeUpCondition = Whitebox.getInternalState(cut, "onPacketAcknowledged");

		Mockito.doAnswer(invocation -> {
			Thread testRunnerThread = Thread.currentThread();
			Thread thread = new Thread(() -> {
				await("Wake up condition to be locked").atMost(5, TimeUnit.SECONDS).until(() -> hasWaiters(lock, wakeUpCondition));
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
		ReentrantLock lock = Whitebox.getInternalState(cut, "notifyLock");
		Condition wakeUpCondition = Whitebox.getInternalState(cut, "onPacketAcknowledged");

		Thread thread = new Thread(() -> {
			await("Wake up condition to be locked").atMost(5, TimeUnit.SECONDS).until(() -> hasWaiters(lock, wakeUpCondition));

			// Speed up time.
			Whitebox.setInternalState(cut, Clock.class, Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(10)));

			lock.lock();
			try {
				wakeUpCondition.signalAll();
			} finally {
				lock.unlock();
			}
		});
		thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Error occurred on unlock thread", e));

		Mockito.doAnswer(invocation -> {
			thread.start();
			return null;
		}).when(multiplexerMock).send((DatagramPacket) notNull());

		clearInterruptionState();

		cut.connect(new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()));
		thread.join(5000);
	}

	private void clearInterruptionState() {
		// Clear the interrupted state if the current thread has it.
		if (Thread.interrupted()) {
			LOGGER.warn("A test failed to clear interrupted state, corrected that.");
		}
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", cut.toString().startsWith("UtpSocketImpl["));
	}

	@Test
	public void testGetOutputStreamExceptionOnUnConnected() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectMessage("not bound");

		cut.getOutputStream();
	}

	@Test
	public void testGetInputStreamExceptionOnUnConnected() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectMessage("not bound");

		cut.getInputStream();
	}

	@Test
	public void testSendOnInterrupt() throws Exception {
		thrown.expect(IOException.class);
		thrown.expectCause(isA(InterruptedException.class));
		thrown.expectMessage("Interruption");

		IPayload payloadMock = mock(IPayload.class);
		when(payloadMock.getSize()).thenReturn(4000);

		ReentrantLock lock = Whitebox.getInternalState(cut, "notifyLock");
		Condition wakeUpCondition = Whitebox.getInternalState(cut, "onPacketAcknowledged");

		final Thread testRunnerThread = Thread.currentThread();
		Thread thread = new Thread(() -> {
			await("Wake up condition to be locked").atMost(5, TimeUnit.SECONDS).until(() -> hasWaiters(lock, wakeUpCondition));
			testRunnerThread.interrupt();
		});
		thread.start();

		clearInterruptionState();

		try {
			cut.send(payloadMock);
		} catch (IOException e) {
			clearInterruptionState();
			throw e;
		}
	}

	@Test
	public void testOnReceivedData() {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);

		when(ackHandlerMock.isInitialised()).thenReturn(true);
		cut.setConnectionState(ConnectionState.CONNECTING);

		cut.onReceivedData();

		assertEquals("Socket state must have transitioned.", ConnectionState.CONNECTED, cut.getConnectionState());
	}

	@Test
	public void testOnReceivedDataNotConnecting() {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);

		when(ackHandlerMock.isInitialised()).thenReturn(true);
		cut.setConnectionState(ConnectionState.DISCONNECTING);

		cut.onReceivedData();

		assertEquals("Socket state must not have transitioned.", ConnectionState.DISCONNECTING, cut.getConnectionState());
	}

	@Test
	public void testOnReceivedDataAckNotInitialised() {
		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);

		when(ackHandlerMock.isInitialised()).thenReturn(false);
		cut.setConnectionState(ConnectionState.CONNECTING);

		cut.onReceivedData();

		assertEquals("Socket state must not have transitioned.", ConnectionState.CONNECTING, cut.getConnectionState());
	}

	@Test
	public void testRepackagePacket() throws Exception {
		injectMultiplexerMock();

		UtpAckHandler ackHandlerMock = mock(UtpAckHandler.class);
		when(ackHandlerMock.countBytesInFlight()).thenReturn(0);
		Whitebox.setInternalState(cut, UtpAckHandler.class, ackHandlerMock);

		cut.setConnectionState(ConnectionState.CONNECTED);

		DataPayload dataPayload = new DataPayload(new byte[200]);
		cut.send(dataPayload);

		// Two packets should have been send.
		verify(multiplexerMock, times(2)).send(anyObject());
	}

	private void hasWaiters(ReentrantLock lock, Condition wakeUpCondition) {
		lock.lock();
		try {
			lock.hasWaiters(wakeUpCondition);
		} finally {
			lock.unlock();
		}
	}

}