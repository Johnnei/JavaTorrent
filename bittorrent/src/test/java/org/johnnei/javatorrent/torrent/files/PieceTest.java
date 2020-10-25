package org.johnnei.javatorrent.torrent.files;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualityMethods;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Piece}
 */
@ExtendWith(TempFolderExtension.class)
public class PieceTest {

	@Test
	public void testStatusCounts() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		assertEquals(10, piece.getBlockCount(), "Incorrect amount of blocks have been created");
		assertEquals(10, piece.countBlocksWithStatus(BlockStatus.Needed), "Incorrect amount of blocks with status Needed");
		assertTrue(piece.hasBlockWithStatus(BlockStatus.Needed), "No piece with Needed status but there should be 10");
		assertFalse(piece.isStarted(), "Piece is started but shouldn't be");
		assertFalse(piece.isDone(), "Piece is done but shouldn't be");
		assertEquals(50, piece.countRemainingBytes(), "Incorrect amount of bytes remaining");
		piece.setBlockStatus(0, BlockStatus.Requested);
		assertTrue(piece.hasBlockWithStatus(BlockStatus.Requested), "No piece with Requested status but there should be 1");
		piece.setBlockStatus(1, BlockStatus.Stored);
		assertTrue(piece.hasBlockWithStatus(BlockStatus.Stored), "No piece with Stored status but there should be 1");
		piece.setBlockStatus(2, BlockStatus.Verified);
		assertTrue(piece.hasBlockWithStatus(BlockStatus.Verified), "No piece with Verified status but there should be 1");
		assertEquals(7, piece.countBlocksWithStatus(BlockStatus.Needed), "Incorrect amount of blocks with status Needed");
		assertEquals(1, piece.countBlocksWithStatus(BlockStatus.Requested), "Incorrect amount of blocks with status Requested");
		assertEquals(1, piece.countBlocksWithStatus(BlockStatus.Stored), "Incorrect amount of blocks with status Stored");
		assertEquals(1, piece.countBlocksWithStatus(BlockStatus.Verified), "Incorrect amount of blocks with status Verified");
		assertEquals(45, piece.countRemainingBytes(), "Incorrect amount of bytes remaining");
		assertTrue(piece.isStarted(), "Piece should be started but isn't");
		assertFalse(piece.isDone(), "Piece shouldn't be done but is.");
		for (int i = 0; i < piece.getBlockCount(); i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
		assertTrue(piece.isStarted(), "Piece should be started but isn't");
		assertTrue(piece.isDone(), "Piece should be done but isn't.");
		assertEquals(0, piece.countRemainingBytes(), "Incorrect amount of bytes remaining");
	}

	@Test
	public void testGetRequestBlock() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		Optional<Block> blockOptional = piece.getRequestBlock();
		assertTrue(blockOptional.isPresent(), "Piece should have returned a block, but didn't");
		assertFalse(piece.isStarted(), "Piece should be started but isn't");

		assertEquals(0, blockOptional.get().getIndex(), "Incorrect piece has been returned");
		assertEquals(5, blockOptional.get().getSize(), "Incorrect block size");
		assertEquals(5, piece.getBlockSize(0), "Incorrect block size");
		assertEquals(BlockStatus.Needed, piece.getBlockStatus(0), "Incorrect block status");
		assertEquals(BlockStatus.Needed, blockOptional.get().getStatus(), "Incorrect block status");
		blockOptional.get().setStatus(BlockStatus.Requested);

		for (int i = 1; i < piece.getBlockCount(); i++) {
			blockOptional = piece.getRequestBlock();
			assertTrue(blockOptional.isPresent(), "Piece should have returned a block, but didn't");
			assertTrue(piece.isStarted(), "Piece should be started but isn't");
			blockOptional.get().setStatus(BlockStatus.Requested);
		}

		assertFalse(piece.getRequestBlock().isPresent(), "Should not have returned a piece after all pieces have been requested");
	}

	@Test
	public void testOnHashFail() {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);

		assertEquals(10, piece.getBlockCount(), "Test relies on 10 pieces. Incorrect piece as starting state.");
		for (int i = 0; i < 10; i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}

		assertTrue(piece.isDone(), "Piece should be done but isn't.");

		for (int i = 0; i < 10; i++) {
			piece.onHashMismatch();
			assertEquals(9, piece.countBlocksWithStatus(BlockStatus.Verified), "Incorrect amount of pieces with status Verified");
			assertEquals(1, piece.countBlocksWithStatus(BlockStatus.Needed), "Incorrect amount of pieces with status Needed");
			assertEquals(BlockStatus.Needed, piece.getBlockStatus(i), "Incorrect status for expected piece to be reset");
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
	}

	@Test
	public void testGetFileSet() {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		Piece piece = new Piece(fileSetMock, new byte[20], 0, 50, 5);
		assertEquals(fileSetMock, piece.getFileSet(), "Incorrect fileset has been returned");
	}

	@Test
	public void testToStringAndEqualsAndHashcode() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 50, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 0, 50, 5);
		Piece pieceThree = new Piece(null, new byte[20], 1, 50, 5);
		assertTrue(pieceOne.toString().startsWith("Piece["), "Incorrect toString start");
		assertEqualityMethods(pieceOne, pieceTwo, pieceThree);
	}

	@ParameterizedTest
	@MethodSource("invalidStatusCalls")
	public void testRangeValidations(Consumer<Piece> consumer) {
		Piece piece = new Piece(null, new byte[20], 0, 50, 5);
		assertThrows(IllegalArgumentException.class, () -> consumer.accept(piece));
	}

	public static Stream<Consumer<Piece>> invalidStatusCalls() {
		return Stream.of(
			piece -> piece.setBlockStatus(-1, BlockStatus.Needed),
			piece -> piece.setBlockStatus(11, BlockStatus.Verified),
			piece -> piece.getBlockStatus(-1),
			piece -> piece.getBlockStatus(11),
			piece -> piece.getBlockSize(-1),
			piece -> piece.getBlockSize(11)
		);
	}

	@Test
	public void testCheckHashOnIncompleteFile(@Folder Path temporaryFolder) throws Exception {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		when(fileSetMock.getBlockSize()).thenReturn(5);
		when(fileSetMock.getPieceSize()).thenReturn(20L);

		FileInfo fileInfo = new FileInfo(20, 0, temporaryFolder.resolve("1").toFile(), 1);
		when(fileSetMock.getFileForBytes(0, 0, 0)).thenReturn(fileInfo);

		Piece cut = new Piece(fileSetMock, new byte[20], 0, 50, 5);

		assertFalse(cut.checkHash(), "Hash should not be matching, but also not throw an exception.");
	}

	@Test
	public void testCheckHashOnIncompleteFileSpanningMultipleFiles(@Folder Path temporaryFolder) throws Exception {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		when(fileSetMock.getBlockSize()).thenReturn(5);
		when(fileSetMock.getPieceSize()).thenReturn(20L);

		FileInfo fileInfoOne = new FileInfo(10, 0, temporaryFolder.resolve("1").toFile(), 1);

		// Ensure that the first file passes the length requirement so the test could fail on the second file.
		fileInfoOne.getFileAccess().seek(0);
		fileInfoOne.getFileAccess().setLength(10);

		FileInfo fileInfoTwo = new FileInfo(10, 10, temporaryFolder.resolve("2").toFile(), 1);
		when(fileSetMock.getFileForBytes(0, 0, 0)).thenReturn(fileInfoOne);
		when(fileSetMock.getFileForBytes(0, 2, 0)).thenReturn(fileInfoTwo);

		Piece cut = new Piece(fileSetMock, new byte[20], 0, 50, 5);

		assertFalse(cut.checkHash(), "Hash should not be matching, but also not throw an exception.");
	}

}
