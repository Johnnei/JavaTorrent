package org.johnnei.javatorrent.disk;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests {@link DiskJobWriteBlock}
 */
@ExtendWith(TempFolderExtension.class)
public class DiskJobWriteBlockTest {

	private CountDownLatch countDownLatch;

	@BeforeEach
	public void setUp() {
		countDownLatch = new CountDownLatch(1);
	}

	@Test
	public void testWriteBlock(@Folder Path tempFolder) throws Exception {
		byte[] bytes = "Hello world write!".getBytes(Charset.forName("UTF-8"));

		File testFile = Files.createFile(tempFolder.resolve("testfile.torrent")).toFile();
			FileInfo fileInfo = new FileInfo(18, 0, testFile, 1);
		AbstractFileSet filesStub = StubEntity.stubAFiles(1, fileInfo);
		Piece piece = new Piece(filesStub, new byte[20], 0, 18, 18);
		DiskJobWriteBlock cut = new DiskJobWriteBlock(piece, 0, bytes, x -> countDownLatch.countDown());
		cut.process();

		byte[] writtenBytes;

		try (ByteInputStream inputStream = new ByteInputStream(new FileInputStream(testFile))) {
			writtenBytes = inputStream.readByteArray(18);
		}

		assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
		assertAll(
			() -> assertArrayEquals(bytes, writtenBytes, "Incorrect output"),
			() -> assertEquals(0, cut.getPriority(), "Incorrect priority"),
			() -> assertEquals(piece, cut.getPiece(), "Incorrect piece"),
			() -> assertEquals(0, cut.getBlockIndex(), "Incorrect block")
		);
	}

}
