package org.johnnei.javatorrent.torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.StringUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TorrentFileSet}
 */
public class TorrentFileSetTest {

	private static final String SINGLE_FILE_TORRENT = "gimp-2.8.16-setup-1.exe.torrent";

	private static final String MULTI_FILE_TORRENT = "my.sql.apache.2.2.php.notepad.torrent";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TorrentFileSet getMultiFileTorrent() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT).toURI());
		return new TorrentFileSet(file, temporaryFolder.newFolder());
	}

	private TorrentFileSet getSingleFileTorrent() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());
		return new TorrentFileSet(file, temporaryFolder.newFolder());
	}

	@Test
	public void testSingleFileTorrentFile() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		assertEquals("Should have had a single file info", 1, cut.getFiles().size());
		assertEquals("Piece size has not been correctly copied from the metadata", 262144, cut.getPieceSize());

		FileInfo fileInfo = cut.getFiles().get(0);
		assertEquals("Filename should have been gimp-2.8.16-setup-1.exe", "gimp-2.8.16-setup-1.exe", fileInfo.getFileName());
		assertEquals("Filesize should have been 743440384", 96823808L, fileInfo.getSize());
		assertEquals("Piece count should have been 1418", 370, fileInfo.getPieceCount());
		assertEquals("First byte offset should have been 0", 0L, fileInfo.getFirstByteOffset());

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT + ".hashes").toURI()))))) {
			for (Piece piece : cut.pieces) {
				String pieceHash = inputStream.readLine();
				byte[] pieceHashBytes = Whitebox.getInternalState(piece, "expectedHash");
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", piece.getIndex()),
						pieceHash,
						StringUtils.byteArrayToString(pieceHashBytes));
			}
		}

		assertEquals("Incorrect amount of bytes to cover all pieces", 47, cut.getBitfieldBytes().length);
		assertEquals("Incorrect amount of blocks in first piece", 16, cut.getPiece(0).getBlockCount());
		assertEquals("Incorrect amount of blocks in last piece", 6, cut.getPiece(cut.getPieceCount() - 1).getBlockCount());
	}

	@Test
	public void testMultiFileTorrentFile() throws Exception {
		TorrentFileSet cut = getMultiFileTorrent();

		assertEquals("Should have had a single file info", 6, cut.getFiles().size());

		final String[] fileNames = {
				"apache_2.2.9-win32-x86-openssl-0.9.8h-r2.msi", "HowToInstallGuide.txt", "httpd.conf",
				"mysql-essential-5.0.51b-win32.msi", "npp.5.6.8.Installer.exe", "php-5.3.2-src.zip"
		};
		final long[] fileSizes = { 5414400, 547, 18124, 23816192, 3336170, 19823435 };
		final int[] pieceCounts = { 83, 1, 1, 365, 52, 304 };
		final long[] firstByteOffsets = { 0, 5414400, 5414947, 5433071, 29249263, 32585433 };

		for (int i = 0; i < cut.getFiles().size(); i++) {
			FileInfo fileInfo = cut.getFiles().get(i);
			assertEquals(String.format("Incorrect filename for entry %d", i), fileNames[i], fileInfo.getFileName());
			assertEquals(String.format("Incorrect file size for entry %d", i), fileSizes[i], fileInfo.getSize());
			assertEquals(String.format("Incorrect piece count for entry %d", i), pieceCounts[i], fileInfo.getPieceCount());
			assertEquals(String.format("Incorrect first byte offset for entry %d", i), firstByteOffsets[i], fileInfo.getFirstByteOffset());
		}

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT + ".hashes").toURI()))))) {
			for (Piece piece : cut.pieces) {
				String pieceHash = inputStream.readLine();
				byte[] pieceHashBytes = Whitebox.getInternalState(piece, "expectedHash");
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", piece.getIndex()),
						pieceHash,
						StringUtils.byteArrayToString(pieceHashBytes));
			}
		}

		assertEquals("Incorrect amount of bytes to cover all pieces", 100, cut.getBitfieldBytes().length);
	}

	@Test
	public void testGetFileForBytes() throws Exception {
		TorrentFileSet cut = getMultiFileTorrent();
		assertEquals("Incorrect block size. Even though the protocol allows it, many clients don't respond on any other size than 1 << 14",
				1 << 14,
				cut.getBlockSize());

		assertEquals("Incorrect file info got returned for the first byte", cut.getFiles().get(0), cut.getFileForBytes(0, 0, 0));
		assertEquals("Incorrect info got returned for the last byte in the first file", cut.getFiles().get(0), cut.getFileForBytes(82, 2, 7679));
		assertEquals("Incorrect file info got returned for the first byte in the second file", cut.getFiles().get(1), cut.getFileForBytes(82, 2, 7680));
	}

	@Test
	public void testGetFileForBytesNegativePiece() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("piece");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(-1, 0, 0);
	}

	@Test
	public void testGetFileForBytesNegativeBlock() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("block");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, -1, 0);
	}

	@Test
	public void testGetFileForBytesNegativeByteOffset() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("byte");
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
		cut.getFileForBytes(0, Integer.MAX_VALUE, 0);
	}

	@Test
	public void testGetFileForBytesOutOfRangeByteOffset() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Byte");
		TorrentFileSet cut = getSingleFileTorrent();
		cut.getFileForBytes(0, 0, Integer.MAX_VALUE);
	}

	@Test
	public void testConstructorNonExistingTorrent() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Torrent file");
		new TorrentFileSet(new File("dfagsdfasjghfh.torrent"), new File("fdafsd"));
	}

	@Test
	public void testConstructorNullTorrent() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Torrent file");
		new TorrentFileSet(null, null);
	}
	@Test
	public void testConstructorNullDownloadFolder() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT).toURI());
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Download folder");
		new TorrentFileSet(file, null);
	}

	@Test
	public void testSetHavingPiece() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		byte[] expectedBitfield = new byte[47];

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

		assertEquals("Incorrect total size", 52408868, cut.getTotalFileSize());
	}

	@Test
	public void testCountRemainingBytes() throws Exception {
		TorrentFileSet cut = getSingleFileTorrent();

		assertEquals("All bytes should have been remaining", 96823808, cut.countRemainingBytes());
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