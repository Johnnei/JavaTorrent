package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TorrentFileSet}
 */
@ExtendWith(TempFolderExtension.class)
public class TorrentFileSetTest {

	private TorrentFileSet getSingleFileTorrent(Path temporaryFolder) throws IOException {
		Metadata metadataMock = mock(Metadata.class);

		when(metadataMock.getPieceSize()).thenReturn(32_768L);
		when(metadataMock.getFileEntries()).thenReturn(Collections.singletonList(new FileEntry("file1.txt", 49_152, 0)));
		when(metadataMock.getPieceHashes()).thenReturn(Arrays.asList(
				DummyEntity.createRandomBytes(20),
				DummyEntity.createRandomBytes(20)
		));

		return new TorrentFileSet(metadataMock, Files.createDirectory(temporaryFolder.resolve("a")).toFile());
	}

	private TorrentFileSet getMultiFileTorrent(Path temporaryFolder) throws IOException {
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

		return new TorrentFileSet(metadataMock, Files.createDirectory(temporaryFolder.resolve("a")).toFile());
	}

	@Test
	public void testGetFileForBytes(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getMultiFileTorrent(tmp);
		assertEquals(
			1 << 14,
			cut.getBlockSize(),
			"Incorrect block size. Even though the protocol allows it, many clients don't respond on any other size than 1 << 14"
		);

		assertEquals(cut.getFiles().get(0), cut.getFileForBytes(0, 0, 0), "Incorrect file info got returned for the first byte");
		assertEquals(cut.getFiles().get(0), cut.getFileForBytes(0, 0, 7679), "Incorrect info got returned for the last byte in the first file");
		assertEquals(cut.getFiles().get(1), cut.getFileForBytes(0, 0, 7680), "Incorrect file info got returned for the first byte in the second file");
	}

	@Test
	public void testGetFileForBytesNegativePiece(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(-1, 0, 0));
		assertThat(e.getMessage(), containsString("Piece"));
	}

	@Test
	public void testGetFileForBytesNegativeBlock(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(0, -1, 0));
		assertThat(e.getMessage(), containsString("Block"));
	}

	@Test
	public void testGetFileForBytesNegativeByteOffset(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(0, 0, -1));
		assertThat(e.getMessage(), containsString("Byte"));
	}

	@Test
	public void testGetFileForBytesOutOfRangePiece(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(Integer.MAX_VALUE, 0, 0));
		assertThat(e.getMessage(), containsString("Piece"));

	}

	@Test
	public void testGetFileForBytesOutOfRangeBlock(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(0, cut.getPieceCount(), 0));
		assertThat(e.getMessage(), containsString("Block"));
	}

	@Test
	public void testGetFileForBytesOutOfRangeByteOffset(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getFileForBytes(0, 0, (int) (cut.getPieceSize())));
		assertThat(e.getMessage(), containsString("Byte"));
	}

	@Test
	public void testConstructorNullTorrent() {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new TorrentFileSet(null, null));
		assertThat(e.getMessage(), containsString("Torrent metadata"));
	}

	@Test
	public void testConstructorNullDownloadFolder() throws Exception {
		Exception e = assertThrows(
			IllegalArgumentException.class,
			() -> new TorrentFileSet(new Metadata.Builder(DummyEntity.createUniqueTorrentHash()).withPieceSize(1).build(), null)
		);
		assertThat(e.getMessage(), containsString("Download folder"));
	}

	@Test
	public void testSetHavingPiece(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);

		byte[] expectedBitfield = new byte[1];

		assertFalse(cut.hasPiece(1), "Piece is completed before interaction.");
		assertArrayEquals(expectedBitfield, cut.getBitfieldBytes(), "Bitfield is dirty before interaction.");

		cut.setHavingPiece(1);

		expectedBitfield[0] = 0x40;

		assertTrue(cut.hasPiece(1), "Piece is not completed after interaction.");
		assertArrayEquals(expectedBitfield, cut.getBitfieldBytes(), "Bitfield has incorrect been altered by interaction.");
	}

	@Test
	public void testHasPieceLowerBound(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		assertFalse(cut.hasPiece(0), "Piece 0 is not yet done.");
		assertThrows(IllegalArgumentException.class, () -> cut.hasPiece(-1));
	}

	@Test
	public void testHasPieceUpperBound(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		assertFalse(cut.hasPiece(cut.getPieceCount() - 1), "Piece is not yet done.");
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.hasPiece(cut.getPieceCount()));
		assertThat(e.getMessage(), containsString("Piece"));
	}

	@Test
	public void testGetPieceLowerBound(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		assertNotNull(cut.getPiece(0), "Piece 0 is not yet done.");
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getPiece(-1));
		assertThat(e.getMessage(), containsString("Piece"));
	}

	@Test
	public void testGetPieceUpperBound(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);
		assertNotNull(cut.getPiece(cut.getPieceCount() - 1), "Piece is not yet done.");
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getPiece(cut.getPieceCount()));
		assertThat(e.getMessage(), containsString("Piece"));
	}

	@Test
	public void testGetTotalFileSize(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getMultiFileTorrent(tmp);

		assertEquals(4194304, cut.getTotalFileSize(), "Incorrect total size");
	}

	@Test
	public void testCountRemainingBytes(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);

		assertEquals(49_152, cut.countRemainingBytes(), "All bytes should have been remaining");
	}

	@Test
	public void testCountCompletedPieces(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);

		assertEquals(0, cut.countCompletedPieces(), "No piece should have been downloaded");
	}

	@Test
	public void testIsDone(@Folder Path tmp) throws Exception {
		TorrentFileSet cut = getSingleFileTorrent(tmp);

		assertFalse(cut.isDone(), "Should not have been done yet.");
		assertEquals(cut.getPieceCount(), cut.getNeededPieces().count(), "All pieces should have been needed");

		for (int i = 0; i < cut.getPieceCount(); i++) {
			cut.setHavingPiece(i);
		}

		assertTrue(cut.isDone(), "Should have been done yet.");
		assertEquals(0, cut.getNeededPieces().count(), "None of the pieces should have been needed");
	}
}
