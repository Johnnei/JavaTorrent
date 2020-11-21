package org.johnnei.javatorrent.internal.tracker.udp;

import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.TorrentClientSettings;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.UdpTracker;

import static org.johnnei.javatorrent.test.DummyEntity.createUniqueTorrent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UdpTrackerSocketTest {

	private Thread thread;

	private TorrentClient torrentClientMock = mock(TorrentClient.class);

	private int transactionId;

	private UdpSocketUtils utilsMock = mock(UdpSocketUtils.class);

	private UdpTracker tracker;

	private UdpTrackerSocket cut;

	private int readAttempt;

	// Locks to delay the assertions until the worker thread has completed its work.

	private Lock resultLock;

	private Condition condition;

	/**
	 * The exception thrown on the worker thread if any
	 */
	private Throwable threadException;

	/**
	 * The thread on which the test is being run
	 */
	private Thread testThread;

	private TestClock testClock;

	@BeforeEach
	public void setUp() throws Exception {
		// Thread sync
		resultLock = new ReentrantLock();
		condition = resultLock.newCondition();
		testThread = Thread.currentThread();
		testClock = new TestClock(Clock.systemDefaultZone());

		TorrentClientSettings settings = mock(TorrentClientSettings.class);
		when(settings.getAcceptingPort()).thenReturn(27500);
		when(torrentClientMock.getSettings()).thenReturn(settings);

		// Prepare context
		readAttempt = 0;
		cut = new UdpTrackerSocket.Builder()
				.setTorrentClient(torrentClientMock)
				.setSocketUtils(utilsMock)
				.setClock(testClock)
				.build();
		tracker = new UdpTracker.Builder()
				.setSocket(cut)
				.setTorrentClient(torrentClientMock)
				.setUrl("udp://localhost:80")
				.build();
		thread = new Thread(cut, "UDP Tracker Test Thread");
		thread.setDaemon(true);

		// Obtain information to rethrow exceptions
		thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
			threadException = e;
			testThread.interrupt();
		});
		thread.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		cut.shutdown();
		thread.join();
	}

	@Test
	public void testAnnounceBeforeConnect() throws Exception {
		InStream connectResponse = new InStream(new byte[] {
				// Action
				0x00, 0x00, 0x00, 0x00,
				// Transaction ID
				0x00, 0x00, 0x00, 0x01,
				// Connection ID
				0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
		});

		InStream malformedResponse = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x02,
				0x00, 0x12, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x08,
				0x00, 0x00, 0x00, 0x08,
				0x00, 0x00, 0x00, 0x08
		});

		InStream scrapeResponse = new InStream(new byte[] {
				// Action
				0x00, 0x00, 0x00, 0x02,
				// Transaction ID
				0x00, 0x00, 0x00, 0x02,
				// Seeders
				0x00, 0x00, 0x00, 0x01,
				// Completed
				0x00, 0x00, 0x00, 0x03,
				// Leechers
				0x00, 0x00, 0x00, 0x02
		});

		Torrent torrent = createUniqueTorrent();
		tracker.addTorrent(torrent);

		ScrapeRequest scrapeMessage = new ScrapeRequest(Collections.singletonList(torrent));

		when(torrentClientMock.createUniqueTransactionId()).thenReturn(++transactionId).thenReturn(++transactionId);

		when(utilsMock.read(isA(DatagramSocket.class))).thenReturn(malformedResponse).thenReturn(connectResponse).thenReturn(scrapeResponse);

		cut.submitRequest(tracker, new MessageWrapper(resultLock, condition, scrapeMessage));

		resultLock.lock();
		try {
			// Await at most 10 seconds in case we already missed the wake up call at this point
			condition.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new AssertionError("Worker thread failed.", threadException);
		} finally {
			resultLock.unlock();
		}

		verify(utilsMock, times(2)).write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));

		TorrentInfo info = assertPresent("Torrent should have been registered.", tracker.getInfo(torrent));

		assertAll(
			() -> assertEquals(1, info.getSeeders(), "Incorrect seeder count"),
			() -> assertEquals(2, info.getLeechers(), "Incorrect seeder count"),
			() -> assertEquals("3", info.getDownloadCount(), "Incorrect seeder count")
		);
	}

	@Test
	public void testPacketTimeouts() throws Exception {
		InStream connectResponse = new InStream(new byte[] {
				// Action
				0x00, 0x00, 0x00, 0x00,
				// Transaction ID
				0x00, 0x00, 0x00, 0x01,
				// Connection ID
				0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
		});

		Torrent torrent = createUniqueTorrent();
		tracker.addTorrent(torrent);

		when(torrentClientMock.createUniqueTransactionId()).thenReturn(++transactionId);

		when(utilsMock.read(isA(DatagramSocket.class))).thenAnswer(inv -> {
			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(16)));
			throw new SocketTimeoutException();
		}).thenAnswer(inv -> {
			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(60)));
			throw new SocketTimeoutException();
		}).thenReturn(connectResponse);

		cut.submitRequest(tracker, new MessageWrapper(resultLock, condition, new ConnectionRequest(Clock.systemDefaultZone())));

		resultLock.lock();
		try {
			// Await at most 10 seconds as the test fakes the time passing by offset
			condition.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new AssertionError("Worker thread failed.", threadException);
		} finally {
			resultLock.unlock();
		}

		verify(utilsMock, times(3)).write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));

		assertEquals(81985529216486895L, tracker.getConnection().getId(), "Invalid connection id");
	}

	@Test
	public void testPacketFullyTimedout() throws Exception {
		Torrent torrent = createUniqueTorrent();
		tracker.addTorrent(torrent);

		tracker = new UdpTrackerWrapper(new UdpTracker.Builder()
				.setSocket(cut)
				.setTorrentClient(torrentClientMock)
				.setUrl("udp://localhost:80"),
				resultLock, condition);

		when(torrentClientMock.createUniqueTransactionId()).thenReturn(++transactionId);

		when(utilsMock.read(isA(DatagramSocket.class))).thenAnswer(inv -> {
			Duration newOffset = Duration.ofSeconds((readAttempt + 1) + (15L * (int) Math.pow(2, readAttempt)));
			++readAttempt;

			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), newOffset));
			throw new SocketTimeoutException();
		});

		// Test with a connection request to prevent connection ID timeouts to cause extra calls.
		cut.submitRequest(tracker, new MessageWrapper(resultLock, condition, new ConnectionRequest(Clock.systemDefaultZone())));

		resultLock.lock();
		try {
			// Await at most 10 seconds as the test fakes the time passing by offset
			condition.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new AssertionError("Worker thread failed.", threadException);
		} finally {
			resultLock.unlock();
		}

		verify(utilsMock, times(9)).write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));

		assertTrue(((UdpTrackerWrapper)tracker).failed, "Expected packet failure");
	}

	private static final class MessageWrapper implements IUdpTrackerPayload {

		private final Lock lock;

		private final Condition condition;

		private final IUdpTrackerPayload message;

		public MessageWrapper(Lock lock, Condition condition, IUdpTrackerPayload message) {
			this.lock = lock;
			this.condition = condition;
			this.message = message;
		}

		@Override
		public void writeRequest(OutStream outStream) {
			message.writeRequest(outStream);
		}

		@Override
		public void readResponse(InStream inStream) throws TrackerException {
			message.readResponse(inStream);
		}

		@Override
		public void process(UdpTracker tracker) {
			message.process(tracker);
			lock.lock();
			try {
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public TrackerAction getAction() {
			return message.getAction();
		}

		@Override
		public int getMinimalSize() {
			return message.getMinimalSize();
		}

		@Override
		public String toString() {
			return message.toString();
		}

	}

	private static class UdpTrackerWrapper extends UdpTracker {

		private final Lock lock;

		private final Condition condition;

		private boolean failed;

		public UdpTrackerWrapper(UdpTracker.Builder builder, Lock lock, Condition condition) throws TrackerException {
			super(builder);
			this.lock = lock;
			this.condition = condition;
		}

		@Override
		public void onRequestFailed(IUdpTrackerPayload payload) {
			super.onRequestFailed(payload);
			failed = true;

			lock.lock();
			try {
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}

	}

}
