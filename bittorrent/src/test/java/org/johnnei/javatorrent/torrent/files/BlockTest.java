package org.johnnei.javatorrent.torrent.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link Block}
 */
public class BlockTest {

	@Test
	public void testToString() throws Exception {
		Block block = new Block(5, 24);

		assertAll(
			() -> assertEquals(5, block.getIndex(), "Incorrect index"),
			() -> assertEquals(24, block.getSize(), "Incorrect size"),
			() -> assertEquals(BlockStatus.Needed, block.getStatus(), "Incorrect status"),
			() -> assertTrue(block.toString().startsWith("Block["), "Incorrect toString start")
		);
	}
}
