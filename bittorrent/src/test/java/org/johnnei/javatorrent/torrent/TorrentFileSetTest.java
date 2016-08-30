package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TorrentFileSet}
 */
public class TorrentFileSetTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TorrentFileSet getSingleFileTorrent() throws IOException {
		Metadata metadataMock = mock(Metadata.class);

		when(metadataMock.getPieceSize()).thenReturn(32_768L);
		when(metadataMock.getFileEntries()).thenReturn(Collections.singletonList(new FileEntry("file1.txt", 49_152, 0)));
		when(metadataMock.getPieceHashes()).thenReturn(Arrays.asList(
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20)
		));

		return new TorrentFileSet(metadataMock, temporaryFolder.newFolder());
	}

	private TorrentFileSet getMultiFileTorrent() throws IOException {
		Metadata metadataMock = mock(Metadata.class);

		when(metadataMock.getPieceSize()).thenReturn(4_194_304L);
		when(metadataMock.getFileEntries()).thenReturn(Arrays.asList(
				new FileEntry("file1.txt", 7_680, 0),
				new FileEntry("file2.txt", 2_093_312, 7_680),
				new FileEntry("file3.txt", 2_093_312, 2_100_992)
		));
		when(metadataMock.getPieceHashes()).thenReturn(Arrays.asList(
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20)
		));

		return new TorrentFileSet(metadataMock, temporaryFolder.newFolder());
	}

	@Test
	public void testGetFileForBytes() throws Exception {
		TorrentFileSet cut = getMultiFileTorrent();
		assertEquals("Incorrect block size. Even though the protocol allows it, many clients don't respond on any other size than 1 << 14",
				1 << 14,
				cut.getBlockSize());

		assertEquals("Incorrect file info got returned for the first byte", cut.getFiles().get(0), cut.getFileForBytes(0, 0, 0));
		assertEquals("Incorrect info got returned for the last byte in the first file", cut.getFiles().get(0), cut.getFileForBytes(0, 0, 7679));
		assertEquals("Incorrect file info got returned for the first byte in the second file", cut.getFiles().get(1), cut.getFileForBytes(0, 0, 7680));
	}

	@Test
	public void testGetFileForBytesNegativePiece() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(-1, 0, 0);
	}

	@Test
	public void testGetFileForBytesNegativeBlock() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Block");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, -1, 0);
	}

	@Test
	public void testGetFileForBytesNegativeByteOffset() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Byte");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, 0, -1);
	}

	@Test
	public void testGetFileForBytesOutOfRangePiece() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(Integer.MAX_VALUE, 0, 0);
	}

	@Test
	public void testGetFileForBytesOutOfRangeBlock() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Block");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, cut.getPieceCount(), 0);
	}

	@Test
	public void testGetFileForBytesOutOfRangeByteOffset() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Byte");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, 0, (int) (cut.getPieceSize()));
	}

	@Test
	public void testConstructorNullTorrent() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Torrent metadata");
		new TorrentFileSet(null, null);
	}
	@Test
	public void testConstructorNullDownloadFolder() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Download folder");
		new TorrentFileSet(new Metadata.Builder().setHash(DummyEntity.createUniqueTorrentHash()).build(), null);
	}

	@Test
	public void testSetHavingPiece() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		byte[] expectedBitfield = new byte[1];

		assertFalse("Piece is completed before interaction.", cut.hasPiece(1));
		assertArrayEquals("Bitfield is dirty before interaction.", expectedBitfield, cut.getBitfieldBytes());

		cut.setHavingPiece(1);

		expectedBitfield[0] = 0x40;

		assertTrue("Piece is not completed after interaction.", cut.hasPiece(1));
		assertArrayEquals("Bitfield has incorrect been altered by interaction.", expectedBitfield, cut.getBitfieldBytes());
	}

	@Test
	public void testHasPieceLowerBound() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");

		TorrentFileSet cut = getSingleFileTorrent();
		assertFalse("Piece 0 is not yet done.", cut.hasPiece(0));
		cut.hasPiece(-1);
	}

	@Test
	public void testHasPieceUpperBound() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");

		TorrentFileSet cut = getSingleFileTorrent();
		assertFalse("Piece is not yet done.", cut.hasPiece(cut.getPieceCount() - 1));
		cut.hasPiece(cut.getPieceCount());
	}

	@Test
	public void testGetPieceLowerBound() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");

		TorrentFileSet cut = getSingleFileTorrent();
		assertNotNull("Piece 0 is not yet done.", cut.getPiece(0));
		cut.getPiece(-1);
	}

	@Test
	public void testGetPieceUpperBound() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Piece");

		TorrentFileSet cut = getSingleFileTorrent();
		assertNotNull("Piece is not yet done.", cut.getPiece(cut.getPieceCount() - 1));
		cut.getPiece(cut.getPieceCount());
	}

	@Test
	public void testGetTotalFileSize() throws Exception {
		TorrentFileSet cut = getMultiFileTorrent();

		assertEquals("Incorrect total size", 4194304, cut.getTotalFileSize());
	}

	@Test
	public void testCountRemainingBytes() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		assertEquals("All bytes should have been remaining", 49_152, cut.countRemainingBytes());
	}

	@Test
	public void testCountCompletedPieces() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		assertEquals("No piece should have been downloaded", 0, cut.countCompletedPieces());
	}

	@Test
	public void testIsDone() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		assertFalse("Should not have been done yet.", cut.isDone());
		assertEquals("All pieces should have been needed", cut.getPieceCount(), cut.getNeededPieces().count());

		for (int i = 0; i < cut.getPieceCount(); i++) {
			cut.setHavingPiece(i);
		}

		assertTrue("Should have been done yet.", cut.isDone());
		assertEquals("None of the pieces should have been needed", 0, cut.getNeededPieces().count());
	}
}