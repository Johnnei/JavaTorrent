package org.johnnei.javatorrent.it;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.phases.PhaseSeed;
import org.johnnei.javatorrent.torrent.DownloadTorrentIT;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(TempFolderExtension.class)
public abstract class EndToEndDownload {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadTorrentIT.class);
	private static final String SINGLE_FILE_TORRENT = "/torrent-files/gimp-2.8.16-setup-1.exe.torrent";
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

	private void assertPreconditions(File torrentFile, File resultFile) throws Exception {
		if (!resultFile.exists()) {
			fail("Missing torrent output file: " + resultFile.getAbsolutePath());
		}

		byte[] bytes = new byte[(int) resultFile.length()];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(resultFile))) {
			inputStream.readFully(bytes);
	}

		assertArrayEquals(EXECUTABLE_HASH, SHA1.hash(bytes), "The output file used to setup the test has a mismatching hash.");

		bytes = new byte[(int) torrentFile.length()];
		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(torrentFile))) {
			inputStream.readFully(bytes);
		}

		assertArrayEquals(TORRENT_FILE_HASH, SHA1.hash(bytes), "The torrent file used to setup the test has a mismatching hash.");
	}

	private void setUpFile(File sourceFile, File outputFile, int startingByte, int length) throws Exception {
		outputFile = new File(outputFile, EXECUTABLE_NAME);
		assertTrue(outputFile.createNewFile(), "Failed to create temporary copy of result file");

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

	protected abstract TorrentClient.Builder createTorrentClient(CountDownLatch latch) throws Exception;

	private TorrentClient finishClient(TorrentClient.Builder builder) throws Exception {
		return builder.setExecutorService(Executors.newScheduledThreadPool(8, r -> {
			Thread t = new Thread(r);
			t.setUncaughtExceptionHandler((thread, e) -> LOGGER.error("Thread {} encountered", thread, e));
			return t;
		})).build();
	}

	private Torrent createTorrent(String name, TorrentClient client, File torrentFile, File downloadFolder) throws IOException {
		return new Torrent.Builder()
			.setTorrentClient(client)
			.setName(name)
			.setDownloadFolder(downloadFolder)
			.setMetadata(Metadata.Builder.from(torrentFile.toPath()).build())
			.build();
	}

	@Test
	public void testDownloadTorrent(@Folder Path tmp) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LOGGER.error("Uncaught exception on thread: {}, Exception", thread, exception));

		URL resultFileUrl = DownloadTorrentIT.class.getResource("/torrent-output/" + EXECUTABLE_NAME);
		File resultFile;

		if (resultFileUrl != null) {
			LOGGER.info("Found torrent output. Location: {}", resultFileUrl);
			resultFile = new File(resultFileUrl.toURI());
		} else {
			throw new IllegalStateException("Missing torrent output file.");
		}

		LOGGER.info("Verifying torrent files to be the correct ones.");
		File torrentFile = new File(DownloadTorrentIT.class.getResource(SINGLE_FILE_TORRENT).toURI());

		assertPreconditions(torrentFile, resultFile);
		LOGGER.info("Setting up test environment with two half completed downloads.");
		File downloadFolderOne = tmp.resolve("client-one").toFile();
		File downloadFolderTwo = tmp.resolve("client-two").toFile();

		assertTrue(downloadFolderOne.mkdirs());
		assertTrue(downloadFolderTwo.mkdirs());

		int halfSize = ((int) (resultFile.length() / PIECE_SIZE) / 2) * PIECE_SIZE;

		setUpFile(resultFile, downloadFolderOne, 0, halfSize);
		setUpFile(resultFile, downloadFolderTwo, halfSize, (int) (resultFile.length() - halfSize));

		LOGGER.info("Preparing torrent clients");
		CountDownLatch latch = new CountDownLatch(2);
		TorrentClient clientOne = finishClient(createTorrentClient(latch));
		TorrentClient clientTwo = finishClient(createTorrentClient(latch));

		try {
			LOGGER.info("Starting downloading");
			LOGGER.debug("[CLIENT ONE] Directory: {}, Port: {}", downloadFolderOne.getAbsolutePath(), clientOne.getSettings().getAcceptingPort());
			LOGGER.debug("[CLIENT TWO] Directory: {}, Port: {}", downloadFolderTwo.getAbsolutePath(), clientTwo.getSettings().getAcceptingPort());
			Torrent torrentOne = createTorrent("GIMP ONE", clientOne, torrentFile, downloadFolderOne);
			Torrent torrentTwo = createTorrent("GIMP TWO", clientTwo, torrentFile, downloadFolderTwo);

			clientOne.download(torrentOne);
			clientTwo.download(torrentTwo);

			assertAll(
				() -> assertEquals(184, torrentOne.getFileSet().countCompletedPieces(), "Incorrect amount of completed pieces for client one"),
				() -> assertEquals(186, torrentTwo.getFileSet().countCompletedPieces(), "Incorrect amount of completed pieces for client two")
			);

			LOGGER.info("Adding peer connect request to client.");
			clientTwo.getPeerConnector().enqueuePeer(new PeerConnectInfo(torrentTwo, new InetSocketAddress("localhost", clientOne.getSettings().getAcceptingPort())));

			LOGGER.info("Waiting for Peers to become connected.");
			await("Peers to connect").atMost(30, TimeUnit.SECONDS).until(() -> !torrentOne.getPeers().isEmpty() && !torrentTwo.getPeers().isEmpty());

			LOGGER.info("Waiting for download completion");
			assertTimeoutPreemptively(Duration.of(5, ChronoUnit.MINUTES), () -> {
				do {
					final int INTERVAL_IN_SECONDS = 10;
					latch.await(INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
					torrentOne.pollRates();
					torrentTwo.pollRates();
					LOGGER.debug(
						"[CLIENT ONE] D: {}KiB/s, U: {}KiB/s, R: {}, [CLIENT TWO] D: {}KiB/s, U: {}KiB/s, R: {}",
						torrentOne.getDownloadRate() / 1024 / INTERVAL_IN_SECONDS,
						torrentOne.getUploadRate() / 1024 / INTERVAL_IN_SECONDS,
						torrentOne.getFileSet().getNeededPieces().count(),
						torrentTwo.getDownloadRate() / 1024 / INTERVAL_IN_SECONDS,
						torrentTwo.getUploadRate() / 1024 / INTERVAL_IN_SECONDS,
						torrentTwo.getFileSet().getNeededPieces().count()
					);

				} while (latch.getCount() > 0);
			});
		} finally {
			clientOne.shutdown();
			clientTwo.shutdown();
		}

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
