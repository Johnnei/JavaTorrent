package org.johnnei.javatorrent.internal.network.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;
import org.johnnei.javatorrent.test.DummyEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
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
		cut = new UtpSocketImpl(multiplexerMock);
	}

	@Test
	public void testCreateConnectingSocket() {
		assertEquals("Incorrect connection id's", cut.getReceivingConnectionId() + 1, cut.getSendingConnectionId());
		assertEquals("Incorrect sequence number", 1, cut.nextSequenceNumber());
		assertEquals("Incorrect connection state", ConnectionState.CONNECTING, cut.getConnectionState());
	}

	@Test
	public void testCreateAcceptingSocket() {
		cut = new UtpSocketImpl(multiplexerMock, new InetSocketAddress("localhost", DummyEntity.findAvailableUdpPort()), (short) 5);

		assertEquals("Incorrect receiving connection id", 6, cut.getReceivingConnectionId());
		assertEquals("Incorrect sending connection id", 5, cut.getSendingConnectionId());
		assertEquals("Incorrect connection state", ConnectionState.CONNECTING, cut.getConnectionState());
	}

	@Test
	public void testConnect() throws Exception {
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

				UtpSocketImpl remoteSocket = mock(UtpSocketImpl.class);
				when(remoteSocket.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
				when(remoteSocket.nextSequenceNumber()).thenReturn((short) 5);

				UtpPacket responsePacket = new UtpPacket(remoteSocket, new StatePayload());

				try {
					cut.process(responsePacket);
				} catch (Exception e) {
					threadException = e;
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