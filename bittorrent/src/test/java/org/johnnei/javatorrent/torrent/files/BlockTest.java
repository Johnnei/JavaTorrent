package org.johnnei.javatorrent.torrent.files;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Block}
 */
public class BlockTest {

	@Test
	public void testToString() throws Exception {
		Block block = new Block(5, 24);

		assertEquals("Incorrect index", 5, block.getIndex());
		assertEquals("Incorrect size", 24, block.getSize());
		assertEquals("Incorrect status", BlockStatus.Needed, block.getStatus());
		assertTrue("Incorrect toString start", block.toString().startsWith("Block["));
	}
}