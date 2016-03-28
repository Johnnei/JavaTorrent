package org.johnnei.javatorrent.disk;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link DiskJobReadBlock}
 */
public class DiskJobReadBlockTest {

	private final File testFile;

	private CountDownLatch countDownLatch;

	public DiskJobReadBlockTest() throws Exception {
		testFile = new File(DiskJobReadBlockTest.class.getResource("readblock.txt").toURI());
	}

	@Before
	public void setUp() {
		countDownLatch = new CountDownLatch(1);
	}

	@Test
	public void testReadBlock() throws Exception {
		FileInfo fileInfo = new FileInfo(11560, 0, testFile, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo, 18);
		Piece piece = new Piece(filesStub, new byte[20], 0, 18, 18);
		DiskJobReadBlock cut = new DiskJobReadBlock(piece, 0, 18, x -> countDownLatch.countDown());
		cut.process();

		assertTrue("Callback method wasn't called", countDownLatch.await(5, TimeUnit.SECONDS));
		assertEquals("Incorrect message read", "Hello world block!", new String(cut.getBlockData(), Charset.forName("UTF-8")));
		assertEquals("Incorrect piece", piece, cut.getPiece());
		assertEquals("Incorrect offset", 0, cut.getOffset());
		assertEquals("Incorrect priority", 10, cut.getPriority());
	}

}