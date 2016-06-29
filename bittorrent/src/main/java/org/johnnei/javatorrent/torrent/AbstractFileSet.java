package org.johnnei.javatorrent.torrent;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.MathUtils;

public abstract class AbstractFileSet {

	/**
	 * The pieces which contain all the information to complete the downloading of the files.
	 */
	protected List<Piece> pieces;

	/**
	 * The file info about all the files which are contained within this {@link AbstractFileSet}
	 */
	protected List<FileInfo> fileInfos;

	/**
	 * The size of a single block within a {@link Piece} in bytes.
	 */
	private final int blockSize;

	public AbstractFileSet(int blockSize) {
		this.blockSize = blockSize;
	}

	/**
	 * Checks if this piece has been completed.
	 *
	 * @param pieceIndex the index of the piece
	 * @return <code>true</code> if the piece has been completed, otherwise <code>false</code>
	 * @throws IllegalArgumentException if the requested piece index is outside of the amount of pieces.
	 */
	public boolean hasPiece(int pieceIndex) {
		Argument.requireWithinBounds(pieceIndex, 0, pieces.size(), String.format("Piece #%d is not within this file set.", pieceIndex));
		return pieces.get(pieceIndex).isDone();
	}

	/**
	 * Marks a piece as completed.
	 *
	 * @param pieceIndex the index of the piece
	 * @throws NoSuchElementException if the requested piece index is outside of the amount of pieces.
	 */
	public void setHavingPiece(int pieceIndex) {
		Piece piece = pieces.get(pieceIndex);
		for (int i = 0; i < piece.getBlockCount(); i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
	}
	/**
	 * Gets the FileInfo for the given piece and block
	 *
	 * @param pieceIndex The piece index
	 * @param blockIndex The block index within the piece
	 * @param byteOffset The offset within the block
	 * @return The FileInfo for the given data
	 * @throws IllegalArgumentException When information being requested is outside of this fileset.
	 */
	public FileInfo getFileForBytes(int pieceIndex, int blockIndex, int byteOffset) {
		validateGetFileForBytes(pieceIndex, blockIndex, byteOffset);
		long bytesStartPosition = (pieceIndex * getPieceSize()) + (blockIndex * getBlockSize()) + byteOffset;

		// Iterate in reverse order so that first file having a smaller first byte offset will be the file containing this section.
		for (int i = fileInfos.size() - 1; i >= 0; i--) {
			FileInfo fileInfo = fileInfos.get(i);
			if (fileInfo.getFirstByteOffset() <= bytesStartPosition) {
				return fileInfo;
			}
		}

		throw new IllegalArgumentException("Piece is not within fileset.");
	}

	private void validateGetFileForBytes(int pieceIndex, int blockIndex, int byteOffset) {
		Argument.requireWithinBounds(pieceIndex, 0, pieces.size(), String.format("Piece %d is not within the file set.", pieceIndex));
		Argument.requirePositive(blockIndex, "Block index cannot be negative.");
		Argument.requirePositive(byteOffset, "Byte offset cannot be negative.");

		if (byteOffset >= getBlockSize()) {
			throw new IllegalArgumentException("Byte offset is out of range (larger or equal to block size).");
		}

		if (blockIndex >= MathUtils.ceilDivision(getPieceSize(), getBlockSize())) {
			throw new IllegalArgumentException("Block index out of range (is larger or equal to piece size).");
		}
	}

	/**
	 * Gets the piece with the given index
	 *
	 * @param index The index of the piece to get
	 * @return The piece at the index
	 * @throws IllegalArgumentException if the index is outside of the files.
	 */
	public Piece getPiece(int index) {
		Argument.requireWithinBounds(index, 0, pieces.size(), String.format("Piece %s is outside of the file set.", index));
		return pieces.get(index);
	}

	/**
	 * Gets the size of a non-truncated piece.
	 *
	 * @return the size of a piece
	 */
	public abstract long getPieceSize();

	/**
	 * Gets the size of a non-truncated block
	 *
	 * @return the size of a block
	 */
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * Tests if all pieces of this fileset have been completed.
	 * @return <code>true</code> when all pieces are done an verified, otherwise <code>false</code>
	 */
	public boolean isDone() {
		return pieces.stream().allMatch(Piece::isDone);
	}

	/**
	 * Creates a stream with only the pieces which are not done
	 *
	 * @return A stream with pieces which need to be downloaded
	 */
	public Stream<Piece> getNeededPieces() {
		return pieces.stream().filter(p -> !p.isDone());
	}

	/**
	 * Gets the amount of pieces in this torrent
	 *
	 * @return The amount of pieces in this file set
	 */
	public int getPieceCount() {
		return pieces.size();
	}

	/**
	 * Gets the sum of the file sizes which are defined in this {@link AbstractFileSet}
	 *
	 * @return The total size of all files in this file set.
	 */
	public long getTotalFileSize() {
		return fileInfos.stream().mapToLong(FileInfo::getSize).sum();
	}

	/**
	 * Calculates the amount of bytes which still need to be downloaded
	 *
	 * @return The amount of bytes still needed to be downloaded
	 */
	public long countRemainingBytes() {
		return pieces.stream().mapToLong(Piece::countRemainingBytes).sum();
	}

	/**
	 * Calculates the amount of pieces which still need to be downloaded
	 *
	 * @return The amount of pieces still needed to be downloaded.
	 */
	public int countCompletedPieces() {
		return (int) pieces.stream().filter(Piece::isDone).count();
	}

	/**
	 * Gets the have pieces in bitfield format as specified by BEP #3.
	 * @return The bitfield associated with this fileset.
	 * @throws UnsupportedOperationException When this implementation doesn't produce the Bitfield as defined in BEP #3.
	 */
	public abstract byte[] getBitfieldBytes();

	/**
	 * Creates an unmodifiable view of the list of files in this fileset.
	 * @return The list of files in this set.
	 */
	public List<FileInfo> getFiles() {
		return Collections.unmodifiableList(fileInfos);
	}

}
