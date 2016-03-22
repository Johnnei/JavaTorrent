package org.johnnei.javatorrent.internal.torrent;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;

import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.expect;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TorrentManager}
 */
public class TorrentManagerTest extends EasyMockSupport {

	@Test
	public void testAddGetTorrent() {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrent);

		TorrentManager cut = new TorrentManager();

		assertNotPresent("Torrent should not have been found yet", cut.getTorrent(torrent.getHashArray()));

		cut.addTorrent(torrent);

		assertPresent("Torrent should have been present", cut.getTorrent(torrent.getHashArray()));
		assertTrue("Collection should have contained torrent", cut.getTorrents().contains(torrent));

		cut.addTorrent(torrentTwo);

		assertEquals("Torrent should have been equal", torrent, cut.getTorrent(torrent.getHashArray()).get());
		assertEquals("Torrent two should have been equal", torrentTwo, cut.getTorrent(torrentTwo.getHashArray()).get());
		assertTrue("Collection should have contained torrent", cut.getTorrents().contains(torrent));
		assertTrue("Collection should have contained torrent two", cut.getTorrents().contains(torrentTwo));
	}

	@Test
	public void testStartStopWithoutPeerConnector() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		replayAll();

		TorrentManager cut = new TorrentManager();

		cut.start(torrentClientMock);

		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(cut, "peerIoRunnable");
		assertNotNull("Peer IO runner should have been started.", peerIoRunnable);

		cut.stop();

		verifyAll();
		assertFalse("Peer IO runner should have been tasked to stop", isRunning(peerIoRunnable));
	}

	@Test
	public void testStartStopWithPeerConnector() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		expect(torrentClientMock.getDownloadPort()).andReturn(DummyEntity.findAvailableTcpPort());
		replayAll();;

		TorrentManager cut = new TorrentManager();


		cut.start(torrentClientMock);
		cut.enableConnectionAcceptor();

		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(cut, "peerIoRunnable");
		LoopingRunnable peerConnectorRunnable = Whitebox.getInternalState(cut, "connectorRunnable");
		assertNotNull("Peer IO runner should have been started.", peerIoRunnable);
		assertNotNull("Peer connector runner should have been started.", peerConnectorRunnable);

		cut.stop();
		verifyAll();

		assertFalse("Peer IO runner should have been tasked to stop", isRunning(peerIoRunnable));
		assertFalse("Peer connector runner should have been tasked to stop", isRunning(peerConnectorRunnable));
	}

	private boolean isRunning(LoopingRunnable runnable) {
		return Whitebox.getInternalState(runnable, "keepRunning");
	}

}