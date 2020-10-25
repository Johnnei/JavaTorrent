package org.johnnei.javatorrent.torrent.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.utils.MathUtils;
import org.johnnei.javatorrent.utils.StringUtils;

/**
 * Represents a piece within a {@link AbstractFileSet}.
 */
public class Piece {

	private static final String ERR_BLOCK_IS_NOT_WITHIN_PIECE = "Block %d is not within the %d blocks of %s";

	/**
	 * The files associated with this piece
	 */
	protected AbstractFileSet files;
	/**
	 * The index of this piece in the torrent
	 */
	private int index;
	/**
	 * All the blocks in this piece
	 */
	private List<Block> blocks;
	/**
	 * The next piece which will be dropped on hash fail
	 */
	private int hashFailCheck;

	private byte[] expectedHash;

	/**
	 * Creates a new piece.
	 * @param files The {@link AbstractFileSet} which owns this piece.
	 * @param hash The hash of the data of this piece.
	 * @param index The piece number within the {@link AbstractFileSet}
	 * @param pieceSize The amount of bytes this piece has.
	 * @param blockSize The size of the blocks.
	 */
	public Piece(AbstractFileSet files, byte[] hash, int index, int pieceSize, int blockSize) {
		this.index = index;
		this.files = files;
		this.expectedHash = hash;
		blocks = new ArrayList<>(MathUtils.ceilDivision(pieceSize, blockSize));
		int blockIndex = 0;
		int remainingPieceSize = pieceSize;
		while (remainingPieceSize > 0) {
			Block block = new Block(blockIndex, Math.min(blockSize, remainingPieceSize));
			remainingPieceSize -= block.getSize();
			blocks.add(block);
			++blockIndex;
		}
	}

	/**
	 * Drops ceil(10%) of the blocks in order to maintain speed and still try to *not* redownload the entire piece
	 */
	public void onHashMismatch() {
		int tenPercent = MathUtils.ceilDivision(blocks.size(), 10);
		for (int i = 0; i < tenPercent; i++) {
			blocks.get(hashFailCheck++).setStatus(BlockStatus.Needed);
			if (hashFailCheck >= blocks.size()) {
				hashFailCheck = 0;
			}
		}
	}

	/**
	 * Loads a bit of data from the file but it is not strictly a block as I use it
	 *
	 * @param offset The offset in the piece
	 * @param length The amount of bytes to read
	 * @return The read bytes or an exception
	 * @throws IOException When the underlying IO causes an error.
	 */
	public byte[] loadPiece(int offset, int length) throws IOException {
		byte[] pieceData = new byte[length];

		int readBytes = 0;
		while (readBytes < length) {
			// Offset within the piece
			int alreadyReadOffset = offset + readBytes;

			// Find file for the given offset
			FileInfo outputFile = files.getFileForBytes(index, alreadyReadOffset / files.getBlockSize(), alreadyReadOffset % files.getBlockSize());

			// Calculate offset as if the torrent was one file
			long pieceIndexOffset = index * files.getPieceSize();
			long totalOffset = pieceIndexOffset + alreadyReadOffset;

			// Calculate the offset within the file
			long offsetInFile = totalOffset - outputFile.getFirstByteOffset();

			// Calculate how many bytes we want/can read from the file
			int bytesToRead = Math.min(length - readBytes, (int) (outputFile.getSize() - offsetInFile));

			// Check if we don't read outside the file
			if (offsetInFile < 0) {
				// TODO Figure out why this exception can be thrown in the first place.
				throw new IOException("Cannot seek to position: " + offsetInFile);
			}

			// Read the actual files
			synchronized (outputFile.fileLock) {
				RandomAccessFile file = outputFile.getFileAccess();
				file.seek(offsetInFile);
				file.readFully(pieceData, readBytes, bytesToRead);
				readBytes += bytesToRead;
			}
		}
		return pieceData;
	}

	/**
	 * Checks if the received bytes hash matches with the hash which was given in the metadata
	 *
	 * @return hashMatched ? true : false
	 */
	public boolean checkHash() throws IOException {
		final int pieceSize = getSize();

		// Test if the piece is completely available on disk.
		int remainingBytes = pieceSize;
		int alreadyReadOffset = 0;
		while (remainingBytes > 0) {
			FileInfo file = files.getFileForBytes(index, alreadyReadOffset / files.getBlockSize(), alreadyReadOffset % files.getBlockSize());

			long pieceIndexOffset = index * files.getPieceSize();
			long totalOffset = pieceIndexOffset + alreadyReadOffset;

			// Calculate the offset within the file
			long offsetInFile = totalOffset - file.getFirstByteOffset();
			long fileSize;
			synchronized (file.fileLock) {
				fileSize = file.getFileAccess().length();
			}

			// Subtract the available bytes.
			long availableBytes = fileSize - offsetInFile;
			if (availableBytes <= 0) {
				// Not enough bytes are available to read this entire piece.
				return false;
			}

			remainingBytes -= availableBytes;
			alreadyReadOffset += availableBytes;
		}

		// Verify the hash.
		byte[] pieceData = loadPiece(0, pieceSize);
		return Arrays.equals(expectedHash, SHA1.hash(pieceData));
	}

	/**
	 * Writes the block into the correct file(s)
	 *
	 * @param blockIndex The index of the block to write
	 * @param blockData The data of the block
	 */
	public void storeBlock(int blockIndex, byte[] blockData) throws IOException {
		Block block = blocks.get(blockIndex);
		int remainingBytesToWrite = block.getSize();
		// Write Block
		while (remainingBytesToWrite > 0) {
			// The offset within the block itself
			int dataOffset = block.getSize() - remainingBytesToWrite;
			// Retrieve the file to which we need to write
			FileInfo outputFile = files.getFileForBytes(index, blockIndex, dataOffset);

			// Calculate the offset in bytes as if the torrent was one file
			final long pieceOffset = index * files.getPieceSize();
			final long blockOffset = (long) blockIndex * files.getBlockSize();
			final long totalOffset = pieceOffset + blockOffset + dataOffset;

			// Calculate the offset within the file
			long offsetInFile = totalOffset - outputFile.getFirstByteOffset();

			// Determine how many bytes still belong in this file
			int bytesToWrite = Math.min(remainingBytesToWrite, (int) (outputFile.getSize() - offsetInFile));

			// Check if the calculated offset is within the file
			if (offsetInFile < 0) {
				throw new IOException("Cannot seek to position: " + offsetInFile);
			}

			// Write the actual bytes
			synchronized (outputFile.fileLock) {
				RandomAccessFile file = outputFile.getFileAccess();
				file.seek(offsetInFile);
				file.write(blockData, dataOffset, bytesToWrite);
				remainingBytesToWrite -= bytesToWrite;
			}
		}
	}

	/**
	 * Counts all block sizes which are not done yet
	 *
	 * @return The remaining amount of bytes to finish this piece
	 */
	public long countRemainingBytes() {
		return blocks.stream().filter(b -> b.getStatus() != BlockStatus.Verified).mapToLong(Block::getSize).sum();
	}

	/**
	 * Updates the block status of the block at the given index.
	 *
	 * @param blockIndex The index of the block.
	 * @param blockStatus The new status of the block.
	 */
	public void setBlockStatus(int blockIndex, BlockStatus blockStatus) {
		if (blockIndex < 0 || blockIndex >= blocks.size()) {
			throw new IllegalArgumentException(String.format(ERR_BLOCK_IS_NOT_WITHIN_PIECE, blockIndex, blocks.size(), this));
		}

		blocks.get(blockIndex).setStatus(blockStatus);
	}

	/**
	 * Gets the block status for the block at the given index
	 *
	 * @param blockIndex The index of the block
	 * @return The status of the given block.
	 */
	public BlockStatus getBlockStatus(int blockIndex) {
		if (blockIndex < 0 || blockIndex >= blocks.size()) {
			throw new IllegalArgumentException(String.format(ERR_BLOCK_IS_NOT_WITHIN_PIECE, blockIndex, blocks.size(), this));
		}

		return blocks.get(blockIndex).getStatus();
	}

	/**
	 * Checks if all blocks are done
	 *
	 * @return If this piece is completed
	 */
	public boolean isDone() {
		return blocks.stream().allMatch(b -> b.getStatus() == BlockStatus.Verified);
	}

	/**
	 * Checks all blockStates if they have been started
	 *
	 * @return true if any progress is found
	 */
	public boolean isStarted() {
		return blocks.stream().anyMatch(b -> b.getStatus() != BlockStatus.Needed);
	}

	/**
	 * Gets the amount of blocks in this piece
	 *
	 * @return block count
	 */
	public int getBlockCount() {
		return blocks.size();
	}

	/**
	 * Gets the index of this Piece<br>
	 * The index is equal to the offset in the file divided by the default piece size
	 *
	 * @return The index of the piece within the fileset.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the total size of all blocks in bytes
	 *
	 * @return The size of this piece
	 */
	public int getSize() {
		return blocks.stream().mapToInt(Block::getSize).sum();
	}

	/**
	 * @param status The status which much be equal.
	 * @return The amount of blocks in this piece with the given status.
	 */
	public int countBlocksWithStatus(BlockStatus status) {
		return (int) blocks.stream().filter(block -> block.getStatus() == status).count();
	}

	/**
	 * Tests if any of the blocks have the given status.
	 *
	 * @param status The status expected
	 * @return returns <code>true</code> when at least 1 block has the given status, otherwise <code>false</code>
	 */
	public boolean hasBlockWithStatus(BlockStatus status) {
		return blocks.stream().anyMatch(block -> block.getStatus() == status);
	}

	/**
	 * Gets a new block to be requested
	 *
	 * @return an unrequested block
	 */
	public Optional<Block> getRequestBlock() {
		return blocks.stream().filter(p -> p.getStatus() == BlockStatus.Needed).findAny();
	}

	/**
	 * Gets the size of the specified block
	 *
	 * @param blockIndex The index of the block to get the size of
	 * @return Size of the block in bytes
	 */
	public int getBlockSize(int blockIndex) {
		if (blockIndex < 0 || blockIndex >= blocks.size()) {
			throw new IllegalArgumentException(String.format(ERR_BLOCK_IS_NOT_WITHIN_PIECE, blockIndex, blocks.size(), this));
		}

		return blocks.get(blockIndex).getSize();
	}

	/**
	 * @return The {@link AbstractFileSet} which contains this piece.
	 */
	public AbstractFileSet getFileSet() {
		return files;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Piece)) {
			return false;
		}

		Piece other = (Piece) obj;

		return index == other.index;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("Piece[index=%d, hash=%s]", index, StringUtils.byteArrayToString(expectedHash));
	}

}
