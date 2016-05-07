package org.johnnei.javatorrent.disk;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link DiskJobCheckHash}
 */
public class DiskJobCheckHashTest {

	private CountDownLatch countDownLatch;

	private final File testFile;
	private final long testFileSize;
	private final File testFileMismatch;

	private final byte[] expectedHash = new byte[] {
			(byte) 0xE9,        0x3C, (byte) 0xF8,        0x55, (byte) 0xC6,
			(byte) 0x96, (byte) 0xB2,        0x06, (byte) 0xB4,        0x54,
			(byte) 0xA0,        0x23,        0x49,        0x01, (byte) 0x99,
			       0x21,( byte) 0xAF,        0x54,        0x1F,        0x7F
	};

	public DiskJobCheckHashTest() throws Exception {
		testFile = new File(DiskJobCheckHashTest.class.getResource("checkhashfile.txt").toURI());
		testFileSize = testFile.length();
		testFileMismatch = new File(DiskJobCheckHashTest.class.getResource("checkhashfile-mismatch.txt").toURI());
	}

	@Before
	public void setUp() {
		countDownLatch = new CountDownLatch(1);
	}

	@Test
	public void testMatchingHash() throws Exception {
		FileInfo fileInfo = new FileInfo(testFileSize, 0, testFile, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo, (int) testFileSize);
		Piece piece = new Piece(filesStub, expectedHash, 0, (int) testFileSize, (int) testFileSize);
		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> countDownLatch.countDown());

		cut.process();
		countDownLatch.await(5, TimeUnit.SECONDS);

		assertTrue("Hash should have matched.", cut.isMatchingHash());
	}

	@Test
	public void testNonMatchingHash() throws Exception {
		FileInfo fileInfo = new FileInfo(testFileSize, 0, testFileMismatch, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo, (int) testFileSize);
		Piece piece = new Piece(filesStub, expectedHash, 0, (int) testFileSize, (int) testFileSize);
		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> countDownLatch.countDown());

		cut.process();
		countDownLatch.await(5, TimeUnit.SECONDS);

		assertFalse("Hash should not have matched.", cut.isMatchingHash());
	}

	@Test
	public void testStaticMethods() {
		Piece piece = new Piece(null, null, 0, 1, 1);

		DiskJobCheckHash cut = new DiskJobCheckHash(piece, x -> {});
		assertEquals("Incorrect piece", piece, cut.getPiece());
		assertEquals("Incorrect priority", 3, cut.getPriority());
	}

}