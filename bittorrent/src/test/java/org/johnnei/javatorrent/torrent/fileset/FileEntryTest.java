package org.johnnei.javatorrent.torrent.fileset;

import org.johnnei.javatorrent.test.TestUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link FileEntry}
 */
public class FileEntryTest {

	@Test
	public void testGetFileName() {
		assertEquals("myFile.txt", new FileEntry("myFile.txt", 42, 7).getFileName());
	}

	@Test
	public void testGetSize() {
		assertEquals(42, new FileEntry("myFile.txt", 42, 7).getSize());
	}

	@Test
	public void testGetFirstByteOffset() {
		assertEquals(7, new FileEntry("myFile.txt", 42, 7).getFirstByteOffset());
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start.", new FileEntry("myFile.txt", 42, 7).toString().startsWith("FileEntry["));
	}

	@Test
	public void testEquality() {
		FileEntry base = new FileEntry("myFile.txt", 42, 7);
		FileEntry equalToBase = new FileEntry("myFile.txt", 42, 7);
		FileEntry notEqualToBase = new FileEntry("myFile.txt", 42, 8);
		FileEntry notEqualToBaseAsWell = new FileEntry("myFile.txt", 41, 7);

		TestUtils.assertEqualityMethods(base, equalToBase, notEqualToBase, notEqualToBaseAsWell);
	}

}