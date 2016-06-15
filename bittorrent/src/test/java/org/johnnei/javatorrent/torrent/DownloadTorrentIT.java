package org.johnnei.javatorrent.torrent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.phases.PhaseSeed;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.tracker.PeerConnector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the ability to cleanly download a torrent.
 */
public class DownloadTorrentIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadTorrentIT.class);

	/* Torrent information */

	private static final String SINGLE_FILE_TORRENT = "gimp-2.8.16-setup-1.exe.torrent";

	private static final byte[] TORRENT_FILE_HASH = new byte[] {
			(byte) 0xc8,        0x36, (byte) 0x9f,        0x0b, (byte) 0xa4, (byte) 0xbf,       0x6c,
			(byte) 0xd8,        0x7f, (byte) 0xb1,        0x3b,        0x34,        0x37,       0x78,
			       0x2e,        0x2c,        0x78,        0x20, (byte) 0xbb,        0x38
	};

	private static final int PIECE_SIZE = 262144;

	private static final String EXECUTABLE_NAME = "gimp-2.8.16-setup-1.exe";
	private static final byte[] EXECUTABLE_HASH = new byte[] {
			(byte) 0x81,        0x6d, (byte) 0xd2,        0x48, (byte) 0xff,        0x18, (byte) 0x82,
			       0x35, (byte) 0xe7, (byte) 0x8b,        0x6d,        0x45, (byte) 0xbf, (byte) 0xec,
			       0x3e,        0x79, (byte) 0xe0, (byte) 0xcc,        0x08, (byte) 0xe1
	};

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public Timeout timeout = new Timeout(20, TimeUnit.MINUTES);

	private void assertPreconditions(File torrentFile, File resultFile) throws Exception {
		if (!resultFile.exists()) {
			fail("Missing torrent output file: " + resultFile.getAbsolutePath());
		}

		byte[] bytes = new byte[(int) resultFile.length()];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(resultFile))) {
			inputStream.readFully(bytes);
		}

		assertArrayEquals("The output file used to setup the test has a mismatching hash.", EXECUTABLE_HASH, SHA1.hash(bytes));

		bytes = new byte[(int) torrentFile.length()];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(torrentFile))) {
			inputStream.readFully(bytes);
		}

		assertArrayEquals("The torrent file used to setup the test has a mismatching hash.", TORRENT_FILE_HASH, SHA1.hash(bytes));
	}

	private void setUpFile(File sourceFile, File folder, int startingByte, int length) throws Exception {
		File outputFile = new File(folder, "gimp-2.8.16-setup-1.exe");
		assertTrue("Failed to create download directoty", outputFile.mkdir());

		outputFile = new File(outputFile, EXECUTABLE_NAME);
		assertTrue("Failed to create temporary copy of result file", outputFile.createNewFile());

		byte[] data = new byte[length];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(sourceFile))) {
			inputStream.skipBytes(startingByte);
			inputStream.readFully(data);
		}

		try (RandomAccessFile fileAccess = new RandomAccessFile(outputFile, "rw")) {
			fileAccess.setLength(sourceFile.length());
			fileAccess.seek(startingByte);
			fileAccess.write(data);
		}
	}

	protected TorrentClient createTorrentClient(CountDownLatch latch) throws Exception {
		return new TorrentClient.Builder()
				.acceptIncomingConnections(true)
				.setConnectionDegradation(new ConnectionDegradation.Builder()
						.registerDefaultConnectionType(TcpSocket.class, TcpSocket::new, Optional.empty())
						.build())
				.setDownloadPort(DummyEntity.findAvailableTcpPort())
				.setExecutorService(Executors.newScheduledThreadPool(2))
				.setPeerConnector(PeerConnector::new)
				.registerTrackerProtocol("stub", (s, torrentClient) -> null)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhaseData.class, PhaseData::new, Optional.of(PhaseSeedCountdown.class))
						.registerPhase(PhaseSeedCountdown.class, ((torrentClient, torrent) -> new PhaseSeedCountdown(latch, torrentClient, torrent)),
								Optional.empty())
						.build()
				).build();
	}

	private Torrent createTorrent(String name, TorrentClient client, File torrentFile, File downloadFolder) {
		Torrent torrent = new Torrent.Builder()
				.setTorrentClient(client)
				.setName(name)
				.setHash(TORRENT_FILE_HASH)
				.build();

		MetadataFileSet metadata = new MetadataFileSet(torrent, torrentFile);
		metadata.getNeededPieces().forEach(p -> metadata.setHavingPiece(p.getIndex()));
		TorrentFileSet fileSet = new TorrentFileSet(torrentFile, downloadFolder);

		torrent.setMetadata(metadata);
		torrent.setFileSet(fileSet);

		return torrent;
	}

	private void downloadTestFile(File resultFile) throws Exception {
		LOGGER.info("Downloading test files...");
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://download.gimp.org/pub/gimp/v2.8/windows/gimp-2.8.16-setup-1.exe")
				.build();
		Response response = client.newCall(request).execute();

		try (FileOutputStream outputStream = new FileOutputStream(resultFile)) {
			InputStream inputStream = response.body().byteStream();

			byte[] buffer = new byte[32768];
			int readBytes;
			while ((readBytes = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, readBytes);
			}
		}
	}

	@Test
	public void testDownloadTorrent() throws Exception {
		URL resultFileUrl = DownloadTorrentIT.class.getResource("/torrent-output/" + EXECUTABLE_NAME);
		File resultFile;

		if (resultFileUrl != null) {
			LOGGER.info("Found cached torrent output, using that. Location: {}", resultFileUrl);
			resultFile = new File(resultFileUrl.toURI());
		} else {
			resultFile = temporaryFolder.newFile();
			downloadTestFile(resultFile);
		}

		LOGGER.info("Verifying torrent files to be the correct ones.");
		File torrentFile = new File(DownloadTorrentIT.class.getResource(SINGLE_FILE_TORRENT).toURI());

		assertPreconditions(torrentFile, resultFile);
		LOGGER.info("Setting up test environment with two half completed downloads.");
		File downloadFolderOne = temporaryFolder.newFolder();
		File downloadFolderTwo = temporaryFolder.newFolder();
		int halfSize = ((int) (resultFile.length() / PIECE_SIZE) / 2) * PIECE_SIZE;

		setUpFile(resultFile, downloadFolderOne, 0, halfSize);
		setUpFile(resultFile, downloadFolderTwo, halfSize, (int) (resultFile.length() - halfSize));

		LOGGER.info("Preparing torrent clients");
		CountDownLatch latch = new CountDownLatch(2);
		TorrentClient clientOne = createTorrentClient(latch);
		TorrentClient clientTwo = createTorrentClient(latch);

		LOGGER.info("Starting downloading");
		LOGGER.debug("[CLIENT ONE] Directory: {}, Port: {}", downloadFolderOne.getAbsolutePath(), clientOne.getDownloadPort());
		LOGGER.debug("[CLIENT TWO] Directory: {}, Port: {}", downloadFolderTwo.getAbsolutePath(), clientTwo.getDownloadPort());
		Torrent torrentOne = createTorrent("GIMP ONE", clientOne, torrentFile, downloadFolderOne);
		Torrent torrentTwo = createTorrent("GIMP TWO", clientTwo, torrentFile, downloadFolderTwo);

		clientOne.download(torrentOne);
		clientTwo.download(torrentTwo);

		assertEquals("Incorrect amount of completed pieces for client one", 184, torrentOne.getFileSet().countCompletedPieces());
		assertEquals("Incorrect amount of completed pieces for client two", 186, torrentTwo.getFileSet().countCompletedPieces());

		LOGGER.info("Adding peer connect request to client.");
		clientTwo.getPeerConnector().enqueuePeer(new PeerConnectInfo(torrentTwo, new InetSocketAddress("localhost", clientOne.getDownloadPort())));

		LOGGER.info("Waiting for download completion");
		do {
			final int INTERVAL_IN_SECONDS = 10;
			latch.await(INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
			torrentOne.pollRates();
			torrentTwo.pollRates();
			LOGGER.debug("[CLIENT ONE] Download: {}KiB/s, Upload: {}KiB/s [CLIENT TWO] Download: {}KiB/s, Upload: {}KiB/s",
					torrentOne.getDownloadRate() / 1024 / INTERVAL_IN_SECONDS,
					torrentOne.getUploadRate() / 1024 / INTERVAL_IN_SECONDS,
					torrentTwo.getDownloadRate() / 1024 / INTERVAL_IN_SECONDS,
					torrentTwo.getUploadRate() / 1024 / INTERVAL_IN_SECONDS);

		} while (latch.getCount() > 0);

		clientOne.shutdown();
		clientTwo.shutdown();
	}

	protected static class PhaseSeedCountdown extends PhaseSeed {

		private final CountDownLatch latch;

		public PhaseSeedCountdown(CountDownLatch latch, TorrentClient torrentClient, Torrent torrent) {
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
