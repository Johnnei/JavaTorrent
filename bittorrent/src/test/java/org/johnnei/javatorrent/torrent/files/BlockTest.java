package org.johnnei.javatorrent.torrent.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

	@Test
	public void testCopyWithStatus() {
		Block base = new Block(3, 15);
		Block request = Block.copyWithStatus(base, BlockStatus.Requested);
		assertAll(
			() -> assertNotEquals(base, request),
			() -> assertEquals(request.getIndex(), 3),
			() -> assertEquals(request.getSize(), 15),
			() -> assertEquals(request.getStatus(), BlockStatus.Requested)
		);
	}
}
