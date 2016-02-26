package org.johnnei.javatorrent.torrent.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.torrent.AFiles;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.utils.MathUtils;

public class Piece {

	/**
	 * The files associated with this piece
	 */
	protected AFiles files;
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

	public Piece(AFiles files, byte[] hash, int index, int pieceSize, int blockSize) {
		this.index = index;
		this.files = files;
		this.expectedHash = hash;
		blocks = new ArrayList<>(MathUtils.ceilDivision(pieceSize, blockSize));
		int blockIndex = 0;
		while (pieceSize > 0) {
			Block block = new Block(blockIndex, Math.min(blockSize, pieceSize));
			pieceSize -= block.getSize();
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
			FileInfo outputFile = files.getFileForBytes(index, 0, alreadyReadOffset);

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
			synchronized (outputFile.FILE_LOCK) {
				RandomAccessFile file = outputFile.getFileAcces();
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
		byte[] pieceData = loadPiece(0, getSize());
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
			final long blockOffset = blockIndex * files.getBlockSize();
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
			synchronized (outputFile.FILE_LOCK) {
				RandomAccessFile file = outputFile.getFileAcces();
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
			throw new IllegalArgumentException(String.format("Block %d is not within the %d blocks of %s", blockIndex, blocks.size(), this));
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
			throw new IllegalArgumentException(String.format("Block %d is not within the %d blocks of %s", blockIndex, blocks.size(), this));
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
	 * Gets the index of this Piece<br/>
	 * The index is equal to the offset in the file divided by the default piece size
	 *
	 * @return The index of the piece within the fileset.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the total size of all blocks
	 *
	 * @return The size of this piece
	 */
	public int getSize() {
		return blocks.stream().mapToInt(Block::getSize).sum();
	}

	public int countBlocksWithStatus(BlockStatus status) {
		return (int) blocks.stream().filter(block -> block.getStatus() == status).count();
	}

	/**
	 * Tests if any of the blocks have the given status.
	 *
	 * @param status The status expected
	 * @return returns <code>true</code> when at least 1 block has the given status, othwise <code>false</code>
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
		Optional<Block> block = blocks.stream().filter(p -> p.getStatus() == BlockStatus.Needed).findAny();

		if (block.isPresent()) {
			block.get().setStatus(BlockStatus.Requested);
		}

		return block;
	}

	/**
	 * Gets the size of the specified block
	 *
	 * @param blockIndex The index of the block to get the size of
	 * @return Size of the block in bytes
	 */
	public int getBlockSize(int blockIndex) {
		if (blockIndex < 0 || blockIndex >= blocks.size()) {
			throw new IllegalArgumentException(String.format("Block %d is not within the %d blocks of %s", blockIndex, blocks.size(), this));
		}

		return blocks.get(blockIndex).getSize();
	}

}
