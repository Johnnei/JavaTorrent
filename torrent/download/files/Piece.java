package torrent.download.files;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.johnnei.utils.JMath;
import org.johnnei.utils.ThreadUtils;

import torrent.TorrentException;
import torrent.download.AFiles;
import torrent.download.FileInfo;
import torrent.encoding.SHA1;

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
	private Block[] blocks;
	/**
	 * The next piece which will be dropped on hash fail
	 */
	private int hashFailCheck;
	
	private byte[] expectedHash;

	public Piece(AFiles files, byte[] hash, int index, int pieceSize, int blockSize) {
		this.index = index;
		this.files = files;
		this.expectedHash = hash;
		blocks = new Block[JMath.ceilDivision(pieceSize, blockSize)];
		int blockOffset = 0;
		while (pieceSize > 0) {
			blocks[blockOffset] = new Block(blockOffset, Math.min(blockSize, pieceSize));
			pieceSize -= blocks[blockOffset].getSize();
			++blockOffset;
		}
	}

	/**
	 * Drops ceil(10%) of the blocks in order to maintain speed and still try to *not* redownload the entire piece
	 */
	public void hashFail() {
		int tenPercent = (int) Math.ceil(blocks.length * 0.1D);
		for (int i = 0; i < tenPercent; i++) {
			reset(hashFailCheck++);
			if (hashFailCheck >= blocks.length) {
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
		blocks[blockIndex].setDone(false);
		blocks[blockIndex].setRequested(false);
	}

	/**
	 * Loads a bit of data from the file but it is not strictly a block as I use it
	 * 
	 * @param offset The offset in the piece
	 * @param remainingBytes The amount of bytes to read
	 * @return The read bytes or an excpetion
	 * @throws TorrentException
	 */
	public byte[] loadPiece(int offset, int remainingBytes) throws TorrentException {
		byte[] blockData = new byte[remainingBytes];
		// Write Block
		while (remainingBytes > 0) {
			int dataOffset = offset + (blockData.length - remainingBytes);
			// Retrieve fileinfo
			FileInfo outputFile = files.getFileForBlock(index, 0, dataOffset);
			long indexOffset = (index * files.getPieceSize());
			long totalOffset = indexOffset + dataOffset;
			long offsetInFile = totalOffset - outputFile.getFirstByteOffset();
			int bytesToRead = remainingBytes;
			if (offsetInFile + remainingBytes > outputFile.getSize()) {
				bytesToRead = (int) (outputFile.getSize() - offsetInFile);
			}
			if (offsetInFile < 0)
				throw new TorrentException("Cannot seek to position: " + offsetInFile);
			// Write Bytes
			synchronized (outputFile.FILE_LOCK) {
				RandomAccessFile file = outputFile.getFileAcces();
				try {
					file.seek(offsetInFile);
					int read = file.read(blockData, (blockData.length - remainingBytes), bytesToRead);
					if (read >= 0)
						remainingBytes -= read;
				} catch (IOException e) {
				}
			}
		}
		return blockData;
	}
	
	/**
	 * Checks if the received bytes hash matches with the hash which was given in the metadata
	 * 
	 * @return hashMatched ? true : false
	 * @throws TorrentException If the piece is not within any of the files in this torrent (Shouldn't occur)
	 */
	public boolean checkHash() throws TorrentException {
		byte[] pieceData = new byte[getSize()];
		int bytesCollected = 0;
		long pieceOffset = getIndex() * files.getPieceSize();
		while (bytesCollected < pieceData.length) {
			int blockIndex = bytesCollected / files.getBlockSize();
			int blockOffset = blockIndex * files.getBlockSize();
			int blockDataOffset = bytesCollected % files.getBlockSize();
			int bytesToRead = getSize() - bytesCollected;
			FileInfo file = files.getFileForBlock(getIndex(), blockIndex, blockDataOffset);
			long offsetInFile = pieceOffset + blockOffset + bytesCollected - file.getFirstByteOffset();
			if (file.getSize() < offsetInFile + bytesToRead) {
				if (offsetInFile >= file.getSize())
					return false;
				bytesToRead = (int) (file.getSize() - offsetInFile);
			}
			synchronized (file.FILE_LOCK) {
				try {
					RandomAccessFile fileAccess = file.getFileAcces();
					fileAccess.seek(offsetInFile);
					int read = fileAccess.read(pieceData, bytesCollected, bytesToRead);
					if (read >= 0)
						bytesCollected += read;
					else if (read == -1) // End of File
						return false;
				} catch (IOException e) {
					ThreadUtils.sleep(10);
					return checkHash();
				}
			}
		}
		return SHA1.match(expectedHash, SHA1.hash(pieceData));
	}

	/**
	 * Writes the block into the correct file(s)
	 * 
	 * @param blockIndex The index of the block to write
	 * @param blockData The data of the block
	 * @throws Exception
	 */
	public void storeBlock(int blockIndex, byte[] blockData) throws TorrentException {
		Block block = blocks[blockIndex];
		if (block.getSize() == blockData.length) {
			int remainingBytes = block.getSize();
			// Write Block
			while (remainingBytes > 0) {
				int dataOffset = (block.getSize() - remainingBytes);
				// Retrieve fileinfo
				FileInfo outputFile = files.getFileForBlock(index, blockIndex, dataOffset);
				long indexOffset = (index * files.getPieceSize());
				int blockOffset = (blockIndex * files.getBlockSize());
				long totalOffset = indexOffset + blockOffset + dataOffset;
				long offsetInFile = totalOffset - outputFile.getFirstByteOffset();
				int bytesToWrite = remainingBytes;
				if (offsetInFile + remainingBytes > outputFile.getSize()) {
					bytesToWrite = (int) (outputFile.getSize() - offsetInFile);
				}
				if (offsetInFile < 0)
					throw new TorrentException("Cannot seek to position: " + offsetInFile);
				// Write Bytes
				synchronized (outputFile.FILE_LOCK) {
					RandomAccessFile file = outputFile.getFileAcces();
					try {
						file.seek(offsetInFile);
						file.write(blockData, dataOffset, bytesToWrite);
						remainingBytes -= bytesToWrite;
					} catch (IOException e) {
						block.setDone(false);
						block.setRequested(false);
					}
				}
				block.setDone(true);
			}
		} else {
			blocks[blockIndex].setDone(false);
			blocks[blockIndex].setRequested(false);
			throw new TorrentException("Block x-" + blockIndex + " size did not match. Expected: " + blocks[blockIndex].getSize() + ", Got: " + blockData.length);
		}
	}

	/**
	 * Counts all block sizes which are not done yet
	 * 
	 * @return The remaining amount of bytes to finish this piece
	 */
	public long getRemainingBytes() {
		long remaining = 0L;
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isDone()) {
				remaining += blocks[i].getSize();
			}
		}
		return remaining;
	}

	/**
	 * Sets the block to done
	 * 
	 * @param blockIndex
	 */
	public void setDone(int blockIndex) {
		blocks[blockIndex].setDone(true);
	}

	public boolean isRequested(int blockIndex) {
		return blocks[blockIndex].isRequested();
	}

	public boolean isDone(int blockIndex) {
		return blocks[blockIndex].isDone();
	}

	/**
	 * Checks if all blocks are done
	 * 
	 * @return If this piece is completed
	 */
	public boolean isDone() {
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isDone())
				return false;
		}
		return true;
	}

	/**
	 * Checks all blockStates if they have been started
	 * 
	 * @return true if any progress is found
	 */
	public boolean isStarted() {
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isStarted())
				return true;
		}
		return false;
	}

	@Override
	public int compareTo(Piece p) {
		int myValue = getCompareValue();
		int theirValue = p.getCompareValue();
		
		return myValue - theirValue;
		
	}
	
	private int getCompareValue() {
		int value = 0;
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isDone())
				value += 2;
			else if (blocks[i].isRequested())
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
		return blocks.length;
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
		int size = 0;
		for (int i = 0; i < blocks.length; i++) {
			size += blocks[i].getSize();
		}
		return size;
	}

	/**
	 * Gets the amount of blocks done
	 * 
	 * @return block done count
	 */
	public int getDoneCount() {
		int doneCount = 0;
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isDone())
				++doneCount;
		}
		return doneCount;
	}

	/**
	 * Gets the amount of block requested
	 * 
	 * @return block requested count
	 */
	public int getRequestedCount() {
		int requestedCount = 0;
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isRequested() && !blocks[i].isDone())
				++requestedCount;
		}
		return requestedCount;
	}

	/**
	 * Gets the amount of block requested including those who are done
	 * 
	 * @return
	 */
	public int getTotalRequestedCount() {
		int requestedCount = 0;
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isRequested())
				++requestedCount;
		}
		return requestedCount;
	}

	/**
	 * Gets a new block to be requested
	 * 
	 * @return an unrequested block
	 */
	public Block getRequestBlock() {
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isDone() && !blocks[i].isRequested()) {
				blocks[i].setRequested(true);
				return blocks[i];
			}
		}
		return null;
	}

	/**
	 * Gets the size of the specified block
	 * 
	 * @param blockIndex The index of the block to get the size of
	 * @return Size of the block in bytes
	 */
	public int getBlockSize(int blockIndex) {
		return blocks[blockIndex].getSize();
	}

}
