package org.johnnei.javatorrent.torrent.download.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.AFiles;
import org.johnnei.javatorrent.torrent.download.FileInfo;
import org.johnnei.javatorrent.torrent.encoding.SHA1;
import org.johnnei.javatorrent.utils.JMath;

public class Piece implements Comparable<Piece> {

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
		blocks = new ArrayList<>(JMath.ceilDivision(pieceSize, blockSize));
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
	public void hashFail() {
		int tenPercent = JMath.ceilDivision(blocks.size(), 10);
		for (int i = 0; i < tenPercent; i++) {
			reset(hashFailCheck++);
			if (hashFailCheck >= blocks.size()) {
				hashFailCheck = 0;
			}
		}
	}

	/**
	 * Resets a single block as unstarted
	 * 
	 * @param blockIndex
	 */
	public void reset(int blockIndex) {
		blocks.get(blockIndex).setDone(false);
		blocks.get(blockIndex).setRequested(false);
	}

	/**
	 * Loads a bit of data from the file but it is not strictly a block as I use it
	 * 
	 * @param offset The offset in the piece
	 * @param length The amount of bytes to read
	 * @return The read bytes or an excpetion
	 * @throws TorrentException
	 */
	public byte[] loadPiece(int offset, int length) throws TorrentException, IOException {
		byte[] pieceData = new byte[length];
		
		int readBytes = 0;
		while (readBytes < length) {
			// Offset within the piece
			int alreadyReadOffset = offset + readBytes;
			
			// Find file for the given offset
			FileInfo outputFile = files.getFileForBytes(index, 0, alreadyReadOffset);
			
			// Calculate offset as if the torrent was one file
			long pieceIndexOffset = (index * files.getPieceSize());
			long totalOffset = pieceIndexOffset + alreadyReadOffset;
			
			// Calculate the offset within the file
			long offsetInFile = totalOffset - outputFile.getFirstByteOffset();
			
			// Calculate how many bytes we want/can read from the file
			int bytesToRead = Math.min(length - readBytes, (int) (outputFile.getSize() - offsetInFile));
			
			// Check if we don't read outside the file
			if (offsetInFile < 0) {
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
	 * @throws TorrentException If the piece is not within any of the files in this torrent (Shouldn't occur)
	 */
	public boolean checkHash() throws TorrentException, IOException {
		byte[] pieceData = loadPiece(0, getSize());
		return SHA1.match(expectedHash, SHA1.hash(pieceData));
	}

	/**
	 * Writes the block into the correct file(s)
	 * 
	 * @param blockIndex The index of the block to write
	 * @param blockData The data of the block
	 * @throws Exception
	 */
	public void storeBlock(int blockIndex, byte[] blockData) throws TorrentException, IOException {
		Block block = blocks.get(blockIndex);
		if (block.getSize() != blockData.length) {
			blocks.get(blockIndex).setDone(false);
			blocks.get(blockIndex).setRequested(false);
			throw new TorrentException("Block size did not match. Expected: " + blocks.get(blockIndex).getSize() + ", Got: " + blockData.length);
		}
		
		int remainingBytesToWrite = block.getSize();
		// Write Block
		while (remainingBytesToWrite > 0) {
			// The offset within the block itself
			int dataOffset = (block.getSize() - remainingBytesToWrite);
			// Retrieve the file to which we need to write
			FileInfo outputFile = files.getFileForBytes(index, blockIndex, dataOffset);
			
			// Calculate the offset in bytes as if the torrent was one file
			final long pieceOffset = (index * files.getPieceSize());
			final long blockOffset = (blockIndex * files.getBlockSize());
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
			
			// Mark the block as done
			block.setDone(true);
		}
	}

	/**
	 * Counts all block sizes which are not done yet
	 * 
	 * @return The remaining amount of bytes to finish this piece
	 */
	public long getRemainingBytes() {
		return blocks.stream().filter(b -> !b.isDone()).mapToLong(Block::getSize).sum();
	}

	/**
	 * Sets the block to done
	 * 
	 * @param blockIndex
	 */
	public void setDone(int blockIndex) {
		blocks.get(blockIndex).setDone(true);
	}

	public boolean isRequested(int blockIndex) {
		return blocks.get(blockIndex).isRequested();
	}

	public boolean isDone(int blockIndex) {
		return blocks.get(blockIndex).isDone();
	}

	/**
	 * Checks if all blocks are done
	 * 
	 * @return If this piece is completed
	 */
	public boolean isDone() {
		return blocks.stream().allMatch(b -> b.isDone());
	}

	/**
	 * Checks all blockStates if they have been started
	 * 
	 * @return true if any progress is found
	 */
	public boolean isStarted() {
		return blocks.stream().anyMatch(b -> b.isStarted());
	}

	@Override
	public int compareTo(Piece p) {
		int myValue = getCompareValue();
		int theirValue = p.getCompareValue();
		
		return myValue - theirValue;
		
	}
	
	private int getCompareValue() {
		int value = 0;
		for (Block block : blocks) {
			if (block.isDone())
				value += 2;
			else if (block.isRequested())
				value += 1;
		}
		return value;
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
	 * @return
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

	/**
	 * Gets the amount of blocks done
	 * 
	 * @return block done count
	 */
	public int getDoneCount() {
		return (int) blocks.stream().filter(p -> p.isDone()).count();
	}

	/**
	 * Gets the amount of block requested
	 * 
	 * @return block requested count
	 */
	public int getRequestedCount() {
		return (int) blocks.stream().filter(p -> p.isRequested() && !p.isDone()).count();
	}

	/**
	 * Gets the amount of block requested including those who are done
	 * 
	 * @return
	 */
	public int getTotalRequestedCount() {
		return (int) blocks.stream().filter(p -> p.isRequested()).count();
	}

	/**
	 * Gets a new block to be requested
	 * 
	 * @return an unrequested block
	 */
	public Block getRequestBlock() {
		Block block = blocks.stream().filter(p -> !p.isStarted()).findAny().orElse(null);
		
		if (block == null) {
			return null;
		} else {
			block.setRequested(true);
			return block;
		}
	}

	/**
	 * Gets the size of the specified block
	 * 
	 * @param blockIndex The index of the block to get the size of
	 * @return Size of the block in bytes
	 */
	public int getBlockSize(int blockIndex) {
		return blocks.get(blockIndex).getSize();
	}

}
