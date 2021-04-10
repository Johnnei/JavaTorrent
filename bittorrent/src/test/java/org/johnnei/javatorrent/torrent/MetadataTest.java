package org.johnnei.javatorrent.torrent;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link Metadata}
 */
public class MetadataTest {

	@Test
	public void testGetHashString() {
		Metadata cut = new Metadata.Builder(DummyEntity.createUniqueTorrentHash())
			.withPieceSize(1)
			.build();

		assertTrue(cut.getHashString().matches("^[a-fA-F0-9]{40}$"), "Incorrect hash output");
	}

	@Test
	public void testGetHash() {
		byte[] hash = DummyEntity.createUniqueTorrentHash();

		Metadata cut = new Metadata.Builder(hash).withPieceSize(1).build();

		assertArrayEquals(hash, cut.getHash(), "Incorrect hash has been returned");
		assertNotEquals(hash, cut.getHash(), "Returned array should be a clone to prevent modification of the hash");
	}

	@Test
	public void testGetNameWithoutNameEntry() {
		byte[] hash = new byte[]{
			0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10,
			0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10
		};

		Metadata cut = new Metadata.Builder(hash).withPieceSize(1).build();

		assertEquals("magnet(1010101010101010101010101010101010101010)", cut.getName(), "Incorrect name format for hash-only name");
	}

	@Test
	public void testEquality() {
		byte[] arrayOne = DummyEntity.createRandomBytes(20);
		byte[] arrayTwo = DummyEntity.createRandomBytes(20);

		Metadata base = new Metadata.Builder(arrayOne).withPieceSize(1).build();
		Metadata equalToBase = new Metadata.Builder(Arrays.copyOf(arrayOne, 20)).withPieceSize(1).build();
		Metadata notEqualToBase = new Metadata.Builder(arrayTwo).withPieceSize(1).build();

		TestUtils.assertEqualityMethods(base, equalToBase, notEqualToBase);
	}

}
