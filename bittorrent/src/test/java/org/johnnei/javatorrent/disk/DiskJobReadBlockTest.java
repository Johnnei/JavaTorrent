package org.johnnei.javatorrent.disk;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests {@link DiskJobReadBlock}
 */
public class DiskJobReadBlockTest {

	private final File testFile;

	private CountDownLatch countDownLatch;

	public DiskJobReadBlockTest() throws Exception {
		testFile = new File(DiskJobReadBlockTest.class.getResource("readblock.txt").toURI());
	}

	@BeforeEach
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

		assertTrue(countDownLatch.await(5, TimeUnit.SECONDS), "Callback method wasn't called");
		assertAll(
			() -> assertEquals("Hello world block!", new String(cut.getBlockData(), Charset.forName("UTF-8")), "Incorrect message read"),
			() -> assertEquals(piece, cut.getPiece(), "Incorrect piece"),
			() -> assertEquals(0, cut.getOffset(), "Incorrect offset"),
			() -> assertEquals(10, cut.getPriority(), "Incorrect priority")
		);
	}

}
