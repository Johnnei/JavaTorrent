package org.johnnei.javatorrent.torrent;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.johnnei.javatorrent.torrent.files.Piece;

public abstract class AFiles {

	/**
	 * The pieces which contain all the information to complete the downloading of the files.
	 */
	protected List<Piece> pieces;

	/**
	 * The file info about all the files which are contained within this {@link AFiles}
	 */
	protected List<FileInfo> fileInfos;

	/**
	 * Checks if this piece has been completed.
	 *
	 * @param pieceIndex the index of the piece
	 * @return <code>true</code> if the piece has been completed, otherwise <code>false</code>
	 * @throws NoSuchElementException if the requested piece index is outside of the amount of pieces.
	 */
	public abstract boolean hasPiece(int pieceIndex) throws NoSuchElementException;

	/**
	 * Marks a piece as completed.
	 *
	 * @param pieceIndex the index of the piece
	 * @throws NoSuchElementException if the requested piece index is outside of the amount of pieces.
	 */
	public abstract void havePiece(int pieceIndex) throws NoSuchElementException;

	/**
	 * Gets the FileInfo for the given piece and block
	 *
	 * @param index The piece index
	 * @param blockIndex The block index within the piece
	 * @param blockDataOffset The offset within the block
	 * @return The FileInfo for the given data
	 */
	public abstract FileInfo getFileForBytes(int index, int blockIndex, int blockDataOffset) throws TorrentException;

	/**
	 * Gets the piece with the given index
	 *
	 * @param index The index of the piece to get
	 * @return The piece at the index
	 * @throws NoSuchElementException if the index is outside of the files.
	 */
	public Piece getPiece(int index) throws NoSuchElementException {
		if (index < 0 || index >= pieces.size()) {
			throw new NoSuchElementException(String.format("%s is outside of the file list.", index));
		}

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
	public abstract int getBlockSize();

	/**
	 * Tests if all pieces of this fileset have been completed.
	 * @return <code>true</code> when all pieces are done an verified, otherwise <code>false</code>
	 */
	public boolean isDone() {
		return pieces.stream().allMatch(Piece::isDone);
	}

	/**
	 * Calculates the amount of pieces which have been completed for a given file.
	 *
	 * @param fileInfo The file in this set of files.
	 * @return The amount of pieces we have of the given file.
	 */
	public int getHavePieceCountForFile(FileInfo fileInfo) {
		int firstPiece = (int) (fileInfo.getFirstByteOffset() / getPieceSize());
		int pieceCount = fileInfo.getPieceCount();

		return (int) pieces.stream()
				.skip(firstPiece)
				.limit(pieceCount)
				.filter(Piece::isDone)
				.count();
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
	 * Gets the sum of the file sizes which are defined in this {@link AFiles}
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
		return pieces.stream().mapToLong(Piece::getRemainingBytes).sum();
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
	 * Calculates the block index based on the given offset.
	 * @param offset The offset within a piece
	 * @return The block index which contains the given offset.
	 */
	public int getBlockIndexByOffset(int offset) {
		return offset / getBlockSize();
	}

	/**
	 * Gets the have pieces in bitfield format as specified by BEP #3.
	 * @return The bitfield associated with this fileset.
	 * @throws UnsupportedOperationException When this implementation doesn't produce the Bitfield as defined in BEP #3.
	 */
	public abstract byte[] getBitfieldBytes() throws UnsupportedOperationException;

	/**
	 * Creates an unmodifiable view of the list of files in this fileset.
	 * @return The list of files in this set.
	 */
	public List<FileInfo> getFiles() {
		return Collections.unmodifiableList(fileInfos);
	}

}
