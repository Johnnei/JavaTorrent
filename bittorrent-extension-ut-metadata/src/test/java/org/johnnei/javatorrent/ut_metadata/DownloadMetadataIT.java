package org.johnnei.javatorrent.ut_metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.magnetlink.MagnetLink;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseMetadata;
import org.johnnei.javatorrent.phases.PhasePreMetadata;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.tracker.PeerConnector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests the integration between all ut_metadata components by downloading a torrent metadata file.
 */
public class DownloadMetadataIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadMetadataIT.class);

	private static final String SINGLE_FILE_TORRENT = "../phases/gimp-2.8.16-setup-1.exe.torrent";

	private static final byte[] TORRENT_FILE_HASH = new byte[] {
			(byte) 0xc8,        0x36, (byte) 0x9f,        0x0b, (byte) 0xa4, (byte) 0xbf,       0x6c,
			(byte) 0xd8,        0x7f, (byte) 0xb1,        0x3b,        0x34,        0x37,       0x78,
			0x2e,        0x2c,        0x78,        0x20, (byte) 0xbb,        0x38
	};

	private static final String METADATA_LINK = "magnet:?dn=GIMP+2.8.16-setup-1.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public Timeout timeout = new Timeout(1, TimeUnit.MINUTES);

	@Test
	public void downloadMetadata() throws Exception {
		LOGGER.info("Verifying expected torrent files to exist.");
		File torrentFile = new File(DownloadMetadataIT.class.getResource(SINGLE_FILE_TORRENT).toURI());

		assertPreconditions(torrentFile);

		LOGGER.info("Setting up test environment.");
		File downloadFolderOne = temporaryFolder.newFolder();
		File downloadFolderTwo = temporaryFolder.newFolder();

		LOGGER.info("Preparing torrent client to download with magnetlink.");
		CountDownLatch latch = new CountDownLatch(1);
		TorrentClient clientWithLink = prepareTorrentClient(downloadFolderOne)
				.setPeerDistributor((t) -> false)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhasePreMetadata.class, PhasePreMetadata::new, Optional.of(PhaseMetadata.class))
						.registerPhase(PhaseMetadata.class, PhaseMetadata::new, Optional.of(PhaseDataCountDown.class))
						.registerPhase(PhaseDataCountDown.class, (client, torrent) -> new PhaseDataCountDown(latch, client, torrent), Optional.empty())
						.build())
				.build();

		LOGGER.info("Preparing torrent client to download with file.");
		TorrentClient clientWithTorrent = prepareTorrentClient(downloadFolderTwo)
				.setPeerDistributor((t) -> false)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhaseData.class, PhaseData::new, Optional.empty())
						.build())
				.build();

		Torrent torrentFromFile = createTorrentFromFile(clientWithTorrent, torrentFile, downloadFolderTwo);
		Torrent torrentFromLink = new MagnetLink(METADATA_LINK, clientWithLink).getTorrent();

		LOGGER.info("Starting downloading");
		LOGGER.debug("[MAGNET ] Directory: {}, Port: {}", downloadFolderOne.getAbsolutePath(), clientWithLink.getDownloadPort());
		LOGGER.debug("[TORRENT] Directory: {}, Port: {}", downloadFolderTwo.getAbsolutePath(), clientWithTorrent.getDownloadPort());

		clientWithTorrent.download(torrentFromFile);
		clientWithLink.download(torrentFromLink);

		LOGGER.info("Adding peer connect request to client.");
		clientWithTorrent.getPeerConnector().enqueuePeer(
				new PeerConnectInfo(torrentFromFile, new InetSocketAddress("localhost", clientWithLink.getDownloadPort())));

		do {
			latch.await(1, TimeUnit.SECONDS);
			torrentFromFile.pollRates();
			torrentFromLink.pollRates();
			LOGGER.debug("[MAGNET ] Download: {}kb/s, Upload: {}kb/s", torrentFromLink.getDownloadRate() / 1024, torrentFromLink.getUploadRate() / 1024);
			LOGGER.debug("[TORRENT] Download: {}kb/s, Upload: {}kb/s", torrentFromFile.getDownloadRate() / 1024, torrentFromFile.getUploadRate() / 1024);
		} while (latch.getCount() > 0);

		clientWithLink.shutdown();
		clientWithTorrent.shutdown();
	}

	private Torrent createTorrentFromFile(TorrentClient torrentClient, File torrentFile, File downloadFolder) {
		Torrent torrent = new Torrent.Builder()
				.setTorrentClient(torrentClient)
				.setName("GIMP")
				.setHash(TORRENT_FILE_HASH)
				.build();

		MetadataFileSet metadata = new MetadataFileSet(torrent, torrentFile);
		metadata.getNeededPieces().forEach(p -> metadata.setHavingPiece(p.getIndex()));
		TorrentFileSet fileSet = new TorrentFileSet(torrentFile, downloadFolder);

		torrent.setMetadata(metadata);
		torrent.setFileSet(fileSet);

		return torrent;
	}

	private TorrentClient.Builder prepareTorrentClient(File downloadFolder) throws Exception {
		return new TorrentClient.Builder()
				.acceptIncomingConnections(true)
				.setConnectionDegradation(new ConnectionDegradation.Builder()
						.registerDefaultConnectionType(TcpSocket.class, TcpSocket::new, Optional.empty())
						.build())
				.setDownloadPort(DummyEntity.findAvailableTcpPort())
				.setExecutorService(Executors.newScheduledThreadPool(2))
				.setPeerConnector(PeerConnector::new)
				.registerModule(new ExtensionModule.Builder()
						.registerExtension(new UTMetadataExtension(downloadFolder, downloadFolder))
						.build())
				.registerTrackerProtocol("stub", (s, torrentClient) -> null);
	}

	private void assertPreconditions(File torrentFile) throws IOException {
		byte[] bytes = new byte[(int) torrentFile.length()];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(torrentFile))) {
			inputStream.readFully(bytes);
		}

		assertArrayEquals("The torrent file used to setup the test has a mismatching hash.", TORRENT_FILE_HASH, SHA1.hash(bytes));
	}

	private static class PhaseDataCountDown extends PhaseData {

		private final CountDownLatch latch;

		PhaseDataCountDown(CountDownLatch latch, TorrentClient torrentClient, Torrent torrent) {
			super(torrentClient, torrent);
			this.latch = latch;
		}

		@Override
		public void onPhaseEnter() {
			latch.countDown();
			super.onPhaseEnter();
		}
	}
}
