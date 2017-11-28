package org.johnnei.javatorrent.internal.torrent.peer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link Bitfield}
 */
public class BitfieldTest {


	@Test
	public void testHavePiece() {
		Bitfield cut = new Bitfield(1);

		cut.havePiece(9, true);

		assertEquals(1, cut.countHavePieces(), "One Piece should have been marked as having");
		assertTrue(cut.hasPiece(9), "Piece 9 should have been marked as having");
		assertEquals(2, cut.getBytes().length, "Byte array should be 2 bytes");

		cut.havePiece(1, true);

		assertEquals(2, cut.countHavePieces(), "Piece should not have been marked as having");
		assertTrue(cut.hasPiece(1), "Piece 1 should have been marked as having");
	}

	@Test
	public void testHavePieceCantExpand() {
		Bitfield cut = new Bitfield(1);

		cut.havePiece(9);

		assertEquals(0, cut.countHavePieces(), "Piece should not have been marked as having");
		assertFalse(cut.hasPiece(9), "Piece 9 should not have been marked as having");

		cut.havePiece(1);

		assertEquals(1, cut.countHavePieces(), "Piece should not have been marked as having");
		assertTrue(cut.hasPiece(1), "Piece 1 should have been marked as having");
	}

	@Test
	public void testSetSize() {
		Bitfield cut = new Bitfield(1);

		assertEquals(1, cut.getBytes().length, "Size should have been 1 byte");

		cut.setSize(1);

		assertEquals(1, cut.getBytes().length, "Size should have been 1 byte");

		cut.setSize(2);

		assertEquals(2, cut.getBytes().length, "Size should have been 1 byte");
	}

}
