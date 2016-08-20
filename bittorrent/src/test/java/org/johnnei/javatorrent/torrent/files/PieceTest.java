package org.johnnei.javatorrent.torrent.files;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualityMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Piece}
 */
public class PieceTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testStatusCounts() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		assertEquals("Incorrect amount of blocks have been created", 10, piece.getBlockCount());
		assertEquals("Incorrect amount of blocks with status Needed", 10, piece.countBlocksWithStatus(BlockStatus.Needed));
		assertTrue("No piece with Needed status but there should be 10", piece.hasBlockWithStatus(BlockStatus.Needed));
		assertFalse("Piece is started but shouldn't be", piece.isStarted());
		assertFalse("Piece is done but shouldn't be", piece.isDone());
		assertEquals("Incorrect amount of bytes remaining", 50, piece.countRemainingBytes());
		piece.setBlockStatus(0, BlockStatus.Requested);
		assertTrue("No piece with Requested status but there should be 1", piece.hasBlockWithStatus(BlockStatus.Requested));
		piece.setBlockStatus(1, BlockStatus.Stored);
		assertTrue("No piece with Stored status but there should be 1", piece.hasBlockWithStatus(BlockStatus.Stored));
		piece.setBlockStatus(2, BlockStatus.Verified);
		assertTrue("No piece with Verified status but there should be 1", piece.hasBlockWithStatus(BlockStatus.Verified));
		assertEquals("Incorrect amount of blocks with status Needed", 7, piece.countBlocksWithStatus(BlockStatus.Needed));
		assertEquals("Incorrect amount of blocks with status Requested", 1, piece.countBlocksWithStatus(BlockStatus.Requested));
		assertEquals("Incorrect amount of blocks with status Stored", 1, piece.countBlocksWithStatus(BlockStatus.Stored));
		assertEquals("Incorrect amount of blocks with status Verified", 1, piece.countBlocksWithStatus(BlockStatus.Verified));
		assertEquals("Incorrect amount of bytes remaining", 45, piece.countRemainingBytes());
		assertTrue("Piece should be started but isn't", piece.isStarted());
		assertFalse("Piece shouldn't be done but is.", piece.isDone());
		for (int i = 0; i < piece.getBlockCount(); i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
		assertTrue("Piece should be started but isn't", piece.isStarted());
		assertTrue("Piece should be done but isn't.", piece.isDone());
		assertEquals("Incorrect amount of bytes remaining", 0, piece.countRemainingBytes());
	}

	@Test
	public void testGetRequestBlock() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		Optional<Block> blockOptional = piece.getRequestBlock();
		assertTrue("Piece should have returned a block, but didn't", blockOptional.isPresent());
		assertTrue("Piece should be started but isn't", piece.isStarted());

		assertEquals("Incorrect piece has been returned", 0, blockOptional.get().getIndex());
		assertEquals("Incorrect block size", 5, blockOptional.get().getSize());
		assertEquals("Incorrect block size", 5, piece.getBlockSize(0));
		assertEquals("Incorrect block status", BlockStatus.Requested, piece.getBlockStatus(0));
		assertEquals("Incorrect block status", BlockStatus.Requested, blockOptional.get().getStatus());

		for (int i = 1; i < piece.getBlockCount(); i++) {
			blockOptional = piece.getRequestBlock();
			assertTrue("Piece should have returned a block, but didn't", blockOptional.isPresent());
			assertTrue("Piece should be started but isn't", piece.isStarted());
		}

		assertFalse("Should not have returned a piece after all pieces have been requested", piece.getRequestBlock().isPresent());
	}

	@Test
	public void testOnHashFail() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		assertEquals("Test relies on 10 pieces. Incorrect piece as starting state.", 10, piece.getBlockCount());
		for (int i = 0; i < 10; i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}

		assertTrue("Piece should be done but isn't.", piece.isDone());

		for (int i = 0; i < 10; i++) {
			piece.onHashMismatch();
			assertEquals("Incorrect amount of pieces with status Verified", 9, piece.countBlocksWithStatus(BlockStatus.Verified));
			assertEquals("Incorrect amount of pieces with status Needed", 1, piece.countBlocksWithStatus(BlockStatus.Needed));
			assertEquals("Incorrect status for expected piece to be reset", BlockStatus.Needed, piece.getBlockStatus(i));
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
	}

	@Test
	public void testGetFileSet() {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		Piece piece = new Piece(fileSetMock, new byte[20], 0, 50, 5);
		assertEquals("Incorrect fileset has been returned", fileSetMock, piece.getFileSet());
	}

	@Test
	public void testToStringAndEqualsAndHashcode() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 50, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 0, 50, 5);
		Piece pieceThree = new Piece(null, new byte[20], 1, 50, 5);
		assertTrue("Incorrect toString start", pieceOne.toString().startsWith("Piece["));
		assertEqualityMethods(pieceOne, pieceTwo, pieceThree);
	}

	@Test
	public void testSetStatusIAEUnderflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.setBlockStatus(-1, BlockStatus.Needed);
	}

	@Test
	public void testSetStatusIAEOverflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.setBlockStatus(11, BlockStatus.Verified);
	}

	@Test
	public void testGetStatusIAEUnderflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.getBlockStatus(-1);
	}

	@Test
	public void testGetStatusIAEOverflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.getBlockStatus(11);
	}

	@Test
	public void testGetSizeIAEUnderflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.getBlockSize(-1);
	}

	@Test
	public void testGetSizeIAEOverflow() {
		thrown.expect(IllegalArgumentException.class);

		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		piece.getBlockSize(11);
	}

	@Test
	public void testCheckHashOnIncompleteFile() throws Exception {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		when(fileSetMock.getBlockSize()).thenReturn(5);
		when(fileSetMock.getPieceSize()).thenReturn(20L);

		FileInfo fileInfo = new FileInfo(20, 0, temporaryFolder.newFile(), 1);
		when(fileSetMock.getFileForBytes(0, 0, 0)).thenReturn(fileInfo);

		Piece cut = new Piece(fileSetMock, new byte[20], 0, 50, 5);

		assertFalse("Hash should not be matching, but also not throw an exception.", cut.checkHash());
	}

	@Test
	public void testCheckHashOnIncompleteFileSpanningMultipleFiles() throws Exception {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		when(fileSetMock.getBlockSize()).thenReturn(5);
		when(fileSetMock.getPieceSize()).thenReturn(20L);

		FileInfo fileInfoOne = new FileInfo(10, 0, temporaryFolder.newFile(), 1);

		// Ensure that the first file passes the length requirement so the test could fail on the second file.
		fileInfoOne.getFileAccess().seek(0);
		fileInfoOne.getFileAccess().setLength(10);

		FileInfo fileInfoTwo = new FileInfo(10, 10, temporaryFolder.newFile(), 1);
		when(fileSetMock.getFileForBytes(0, 0, 0)).thenReturn(fileInfoOne);
		when(fileSetMock.getFileForBytes(0, 2, 0)).thenReturn(fileInfoTwo);

		Piece cut = new Piece(fileSetMock, new byte[20], 0, 50, 5);

		assertFalse("Hash should not be matching, but also not throw an exception.", cut.checkHash());
	}

}