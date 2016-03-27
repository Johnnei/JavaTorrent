package org.johnnei.javatorrent.torrent;

import org.johnnei.javatorrent.test.TestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertTrue;

/**
 * Tests {@link FileInfo}
 */
public class FileInfoTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testEqualsAndHashcode() throws Exception {
		FileInfo base = new FileInfo(0, 0, temporaryFolder.newFile(), 0);
		FileInfo equalToBase = new FileInfo(0, 0, temporaryFolder.newFile(), 0);
		FileInfo notEqualToBase = new FileInfo(1, 0, temporaryFolder.newFile(), 0);
		FileInfo notEqualToBase2 = new FileInfo(0, 1, temporaryFolder.newFile(), 0);

		TestUtils.assertEqualityMethods(base, equalToBase, notEqualToBase, notEqualToBase2);
	}

	@Test
	public void testToString() throws Exception {
		FileInfo base = new FileInfo(0, 0, temporaryFolder.newFile(), 0);
		assertTrue("toString didn't start with class name + [", base.toString().startsWith("FileInfo["));
	}

}