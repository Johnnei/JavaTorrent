package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link FileInfo}
 */
@ExtendWith(TempFolderExtension.class)
public class FileInfoTest {

	@Test
	public void testEqualsAndHashcode(@Folder Path temporaryFolder) throws Exception {
		FileInfo base = new FileInfo(0, 0, temporaryFolder.resolve("a").toFile(), 0);
		FileInfo equalToBase = new FileInfo(0, 0, temporaryFolder.resolve("b").toFile(), 0);
		FileInfo notEqualToBase = new FileInfo(1, 0, temporaryFolder.resolve("c").toFile(), 0);
		FileInfo notEqualToBase2 = new FileInfo(0, 1, temporaryFolder.resolve("d").toFile(), 0);

		TestUtils.assertEqualityMethods(base, equalToBase, notEqualToBase, notEqualToBase2);
	}

	@Test
	public void testGetFileName(@Folder Path temporaryFolder) throws IOException {
		FileInfo cut = new FileInfo(0, 0, temporaryFolder.resolve("test-file.txt").toFile(), 0);
		assertEquals("test-file.txt", cut.getFileName(), "Incorrect file name has been returned");
	}

	@Test
	public void testGetPieceCount(@Folder Path temporaryFolder) throws IOException {
		FileInfo cut = new FileInfo(0, 0, temporaryFolder.resolve("a").toFile(), 42);
		assertEquals(42, cut.getPieceCount(), "Incorrect amount of pieces have been returned");
	}

	@Test
	public void testToString(@Folder Path temporaryFolder) throws Exception {
		FileInfo base = new FileInfo(0, 0, temporaryFolder.resolve("a").toFile(), 0);
		assertTrue(base.toString().startsWith("FileInfo["), "toString didn't start with class name + [");
	}

}
