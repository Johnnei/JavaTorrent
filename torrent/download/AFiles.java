package torrent.download;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import torrent.TorrentException;
import torrent.download.files.Piece;

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
	 * @param pieceIndex the index of the piece
	 * @return <code>true</code> if the piece has been completed, otherwise <code>false</code>
	 * @throws NoSuchElementException if the requested piece index is outside of the amount of pieces.
	 */
	public abstract boolean hasPiece(int pieceIndex) throws NoSuchElementException;
	
	/**
	 * Marks a piece as completed.
	 * @param pieceIndex the index of the piece
	 * @return <code>true</code> if the piece has been completed, otherwise <code>false</code>
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
	 * @return the size of a piece
	 */
	public abstract long getPieceSize();
	
	/**
	 * Gets the size of a non-truncated block
	 * @return the size of a block
	 */
	public abstract int getBlockSize();
	
	public boolean isDone() {
		return !pieces.stream().filter(p -> !p.isDone()).findAny().isPresent();
	}
	
	/**
	 * Creates a stream with only the pieces which are not done
	 * @return A stream with pieces which need to be downloaded
	 */
	public Stream<Piece> getNeededPieces() {
		return pieces.stream().filter(p -> !p.isDone());
	}
	
	/**
	 * Gets the amount of pieces in this torrent
	 * @return
	 */
	public int getPieceCount() {
		return pieces.size();
	}
	
	/**
	 * Gets the sum of the file sizes which are defined in this {@link AFiles}
	 * @return
	 */
	public long getTotalFileSize() {
		return fileInfos.stream().mapToLong(FileInfo::getSize).sum();
	}

	/**
	 * The amount of bytes still needed to be downloaded
	 * 
	 * @return
	 */
	public long countRemainingBytes() {
		return pieces.stream().mapToLong(Piece::getRemainingBytes).sum();
	}
	
	public int countCompletedPieces() {
		return (int) pieces.stream().filter(p -> p.isDone()).count();
	}

	public int getBlockIndexByOffset(int offset) {
		return offset / getBlockSize();
	}
	
	public abstract byte[] getBitfieldBytes() throws UnsupportedOperationException;
	
	public Collection<FileInfo> getFiles() {
		return fileInfos;
	}

}
