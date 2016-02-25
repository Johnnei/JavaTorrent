package org.johnnei.javatorrent.disk;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.network.ByteInputStream;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AFiles;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link DiskJobWriteBlock}
 */
public class DiskJobWriteBlockTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private CountDownLatch countDownLatch;

	@Before
	public void setUp() {
		countDownLatch = new CountDownLatch(1);
	}

	@Test
	public void testWriteBlock() throws Exception {
		byte[] bytes = "Hello world write!".getBytes(Charset.forName("UTF-8"));

		File testFile = tempFolder.newFile();
		FileInfo fileInfo = new FileInfo(18, 0, testFile, 1);
		AFiles filesStub = StubEntity.stubAFiles(1, fileInfo);
		Piece piece = new Piece(filesStub, new byte[20], 0, 18, 18);
		DiskJobWriteBlock cut = new DiskJobWriteBlock(piece, 0, bytes, x -> countDownLatch.countDown());
		cut.process();

		byte[] writtenBytes;

		try (ByteInputStream inputStream = new ByteInputStream(new FileInputStream(testFile))) {
			writtenBytes = inputStream.readByteArray(18);
		}

		assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
		assertArrayEquals("Incorrect output", bytes, writtenBytes);
		assertEquals("Incorrect priority", 0, cut.getPriority());
		assertEquals("Incorrect piece", piece, cut.getPiece());
		assertEquals("Incorrect block", 0, cut.getBlockIndex());
	}

}