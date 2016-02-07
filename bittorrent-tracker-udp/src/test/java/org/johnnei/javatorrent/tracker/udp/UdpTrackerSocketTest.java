package org.johnnei.javatorrent.tracker.udp;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.notNull;
import static org.johnnei.javatorrent.test.DummyEntity.createTorrent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.RulePrintTestCase;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.torrent.tracker.TrackerManager;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class UdpTrackerSocketTest extends EasyMockSupport {

	@Rule
	public RulePrintTestCase printCases = new RulePrintTestCase();

	private Thread thread;

	private TorrentClient torrentClientMock = createMock(TorrentClient.class);

	private int transactionId;

	private TrackerManager trackerManagerMock = createMock(TrackerManager.class);

	private UdpSocketUtils utilsMock = createMock(UdpSocketUtils.class);

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

	@Before
	public void setUp() throws Exception {
		// Thread sync
		resultLock = new ReentrantLock();
		condition = resultLock.newCondition();
		testThread = Thread.currentThread();
		testClock = new TestClock(Clock.systemDefaultZone());

		// Prepare context
		readAttempt = 0;
		cut = new UdpTrackerSocket.Builder()
				.setTrackerManager(trackerManagerMock)
				.setSocketPort(27500)
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

	@After
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

		Torrent torrent = createTorrent();
		tracker.addTorrent(torrent);

		ScrapeRequest scrapeMessage = new ScrapeRequest(Collections.singletonList(torrent));

		expect(trackerManagerMock.createUniqueTransactionId()).andReturn(++transactionId);
		expect(trackerManagerMock.createUniqueTransactionId()).andReturn(++transactionId);

		utilsMock.write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));
		expectLastCall().times(2);

		expect(utilsMock.read(isA(DatagramSocket.class))).andReturn(malformedResponse);
		expect(utilsMock.read(isA(DatagramSocket.class))).andReturn(connectResponse);
		expect(utilsMock.read(isA(DatagramSocket.class))).andReturn(scrapeResponse);

		replayAll();

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

		verifyAll();

		TorrentInfo info = tracker.getInfo(torrent).get();

		assertEquals("Incorrect seeder count", 1, info.getSeeders());
		assertEquals("Incorrect seeder count", 2, info.getLeechers());
		assertEquals("Incorrect seeder count", "3", info.getDownloadCount());
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

		Torrent torrent = createTorrent();
		tracker.addTorrent(torrent);

		expect(trackerManagerMock.createUniqueTransactionId()).andReturn(++transactionId);

		utilsMock.write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));
		expectLastCall().times(3);

		expect(utilsMock.read(isA(DatagramSocket.class))).andAnswer(() -> {
			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(16)));
			throw new SocketTimeoutException();
		});

		expect(utilsMock.read(isA(DatagramSocket.class))).andAnswer(() -> {
			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(60)));
			throw new SocketTimeoutException();
		});

		expect(utilsMock.read(isA(DatagramSocket.class))).andReturn(connectResponse);

		replayAll();

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

		verifyAll();

		assertEquals("Invalid connection id", 81985529216486895L, tracker.getConnection().getId());
	}

	@Test
	public void testPacketFullyTimedout() throws Exception {
		Torrent torrent = createTorrent();
		tracker.addTorrent(torrent);

		tracker = new UdpTrackerWrapper(new UdpTracker.Builder()
				.setSocket(cut)
				.setTorrentClient(torrentClientMock)
				.setUrl("udp://localhost:80"),
				resultLock, condition);

		expect(trackerManagerMock.createUniqueTransactionId()).andReturn(++transactionId);

		utilsMock.write(isA(DatagramSocket.class), notNull(), isA(OutStream.class));
		expectLastCall().times(9);

		expect(utilsMock.read(isA(DatagramSocket.class))).andAnswer(() -> {
			Duration newOffset = Duration.ofSeconds((readAttempt + 1) + (15L * (int) Math.pow(2, readAttempt)));
			++readAttempt;

			testClock.setClock(Clock.offset(Clock.systemDefaultZone(), newOffset));
			throw new SocketTimeoutException();
		}).times(9);

		replayAll();

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

		verifyAll();

		assertTrue("Expected packet failure", ((UdpTrackerWrapper)tracker).failed);
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
