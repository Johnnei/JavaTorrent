package org.johnnei.javatorrent.internal.torrent.peer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Bitfield}
 */
public class BitfieldTest {


	@Test
	public void testHavePiece() {
		Bitfield cut = new Bitfield(1);

		cut.havePiece(9, true);

		assertEquals("One Piece should have been marked as having", 1, cut.countHavePieces());
		assertTrue("Piece 9 should have been marked as having", cut.hasPiece(9));
		assertEquals("Byte array should be 2 bytes", 2, cut.getBytes().length);

		cut.havePiece(1, true);

		assertEquals("Piece should not have been marked as having", 2, cut.countHavePieces());
		assertTrue("Piece 1 should have been marked as having", cut.hasPiece(1));
	}

	@Test
	public void testHavePieceCantExpand() {
		Bitfield cut = new Bitfield(1);

		cut.havePiece(9);

		assertEquals("Piece should not have been marked as having", 0, cut.countHavePieces());
		assertFalse("Piece 9 should not have been marked as having", cut.hasPiece(9));

		cut.havePiece(1);

		assertEquals("Piece should not have been marked as having", 1, cut.countHavePieces());
		assertTrue("Piece 1 should have been marked as having", cut.hasPiece(1));
	}

	@Test
	public void testSetSize() {
		Bitfield cut = new Bitfield(1);

		assertEquals("Size should have been 1 byte", 1, cut.getBytes().length);

		cut.setSize(1);

		assertEquals("Size should have been 1 byte", 1, cut.getBytes().length);

		cut.setSize(2);

		assertEquals("Size should have been 1 byte", 2, cut.getBytes().length);
	}

}