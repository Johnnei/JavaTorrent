package org.johnnei.javatorrent.disk;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests {@link DiskJobCheckHash}
 */
public class DiskJobCheckHashTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobCheckHashTest.class);

	private CountDownLatch countDownLatch;

	private final File testFile;
	private final long testFileSize;
	private final File testFileMismatch;
	private final long testFileMismatchSize;

	private final byte[] expectedHash = new byte[] {
			(byte) 0x11, (byte) 0xfb, (byte) 0x97, (byte) 0x0c,
			(byte) 0xfe, (byte) 0x8e, (byte) 0xcd, (byte) 0x89,
			(byte) 0xaf, (byte) 0xa7, (byte) 0x50, (byte) 0xd0,
			(byte) 0x64, (byte) 0x54, (byte) 0xdc, (byte) 0xd1,
			(byte) 0x60, (byte) 0xd7, (byte) 0x5e, (byte) 0x19
	};

	public DiskJobCheckHashTest() throws Exception {
		testFile = new File(DiskJobCheckHashTest.class.getResource("checkhashfile.txt").toURI());
		testFileSize = testFile.length();
		testFileMismatch = new File(DiskJobCheckHashTest.class.getResource("checkhashfile-mismatch.txt").toURI());
		testFileMismatchSize = testFileMismatch.length();
	}

	@BeforeEach
	public void setUp() {
		countDownLatch = new CountDownLatch(1);

		LOGGER.info("Correct hash file: {} bytes", testFileSize);
		LOGGER.info("Mismatch hash file: {} bytes", testFileMismatchSize);
	}

	@Test
	public void testMatchingHash() throws Exception {
		FileInfo fileInfo = new FileInfo(testFileSize, 0, testFile, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo, (int) testFileSize);
		Piece piece = new Piece(filesStub, expectedHash, 0, (int) testFileSize, (int) testFileSize);
		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> countDownLatch.countDown());

		cut.process();
		countDownLatch.await(5, TimeUnit.SECONDS);

		assertTrue(cut.isMatchingHash(), "Hash should have matched.");
	}

	@Test
	public void testNonMatchingHash() throws Exception {
		FileInfo fileInfo = new FileInfo(testFileSize, 0, testFileMismatch, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo, (int) testFileSize);
		Piece piece = new Piece(filesStub, expectedHash, 0, (int) testFileSize, (int) testFileSize);
		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> countDownLatch.countDown());

		cut.process();
		countDownLatch.await(5, TimeUnit.SECONDS);

		assertFalse(cut.isMatchingHash(), "Hash should not have matched.");
	}

	@Test
	public void testStaticMethods() {
		Piece piece = new Piece(null, null, 0, 1, 1);

		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> {});
		assertAll(
			() -> assertEquals(piece, cut.getPiece(), "Incorrect piece"),
			() -> assertEquals(3, cut.getPriority(), "Incorrect priority")
		);
	}

}
