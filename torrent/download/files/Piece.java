package torrent.download.files;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.johnnei.utils.JMath;

import torrent.TorrentException;
import torrent.download.FileInfo;
import torrent.download.Files;
import torrent.util.ISortable;

public class Piece implements ISortable {
	
	/**
	 * The files associated with this piece
	 */
	protected Files files;
	/**
	 * The index of this piece in the torrent
	 */
	private int index;
	/**
	 * All the blocks in this piece
	 */
	private Block[] blocks;
	
	public Piece(Files files, int index, int pieceSize, int blockSize) {
		this.index = index;
		this.files = files;
		blocks = new Block[JMath.ceil(pieceSize / (double)blockSize)];
		int blockOffset = 0;
		while(pieceSize > 0) {
			blocks[blockOffset] = new Block(blockOffset, JMath.min(blockSize, pieceSize));
			pieceSize -= blocks[blockOffset].getSize();
			++blockOffset;
		}
	}
	
	/**
	 * Reset the entire piece as unstarted
	 */
	public void reset() {
		for(int i = 0; i < blocks.length; i++) {
			blocks[i].setDone(false);
			blocks[i].setRequested(false);
		}
	}
	
	/**
	 * Resets a single block as unstarted
	 * @param blockIndex
	 */
	public void reset(int blockIndex) {
		blocks[blockIndex].setDone(false);
		blocks[blockIndex].setRequested(false);
	}
	
	/**
	 * Writes the block into the correct file(s)
	 * @param blockIndex The index of the block to write
	 * @param blockData The data of the block
	 * @throws Exception
	 */
	public void storeBlock(int blockIndex, byte[] blockData) throws TorrentException {
		Block block = blocks[blockIndex];
		if(block.getSize() == blockData.length) {
			int remainingBytes = block.getSize();
			//Write Block
			while(remainingBytes > 0) {
				int dataOffset = (block.getSize() - remainingBytes);
				//Retrieve fileinfo
				FileInfo outputFile = files.getFileForBlock(index, blockIndex, dataOffset);
				long totalOffset = (index * files.getPieceSize()) + (blockIndex * files.getBlockSize()) + dataOffset;
				long offsetInFile = totalOffset - outputFile.getFirstByteOffset();
				int bytesToWrite = remainingBytes;
				if(offsetInFile + remainingBytes > outputFile.getSize()) {
					bytesToWrite = (int)(outputFile.getSize() - offsetInFile);
				}
				//Write Bytes
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
			throw new TorrentException("Block x-"+ blockIndex +" size did not match. Expected: " + blocks[blockIndex].getSize() + ", Got: " + blockData.length);
		}
	}
	
	/**
	 * Counts all block sizes which are not done yet
	 * @return The remaining amount of bytes to finish this piece
	 */
	public long getRemainingBytes() {
		long remaining = 0L;
		for(int i = 0; i < blocks.length; i++) {
			if(!blocks[i].isDone()) {
				remaining += blocks[i].getSize();
			}
		}
		return remaining;
	}
	
	/**
	 * Checks if all blocks are done
	 * @return If this piece is completed
	 */
	public boolean isDone() {
		for(int i = 0; i < blocks.length; i++) {
			if(!blocks[i].isDone())
				return false;
		}
		return true;
	}
	
	/**
	 * Checks all blockStates if they have been started
	 * @return true if any progress is found
	 */
	public boolean isStarted() {
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isStarted())
				return true;
		}
		return false;
	}

	@Override
	public int getValue() {
		int value = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isDone())
				value += 2;
			else if(blocks[i].isRequested())
				value += 1;
		}
		return value;
	}

	/**
	 * Gets the amount of blocks in this piece
	 * @return block count
	 */
	public int getBlockCount() {
		return blocks.length;
	}
	
	/**
	 * Gets the index of this Piece<br/>
	 * The index is equal to the offset in the file divided by the default piece size
	 * @return
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the total size of all blocks
	 * @return The size of this piece
	 */
	public int getSize() {
		int size = 0;
		for(int i = 0; i < blocks.length; i++) {
			size += blocks[i].getSize();
		}
		return size;
	}

	/**
	 * Gets the amount of blocks done
	 * @return block done count
	 */
	public int getDoneCount() {
		int doneCount = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isDone())
				++doneCount;
		}
		return doneCount;
	}

	/**
	 * Gets the amount of block requested
	 * @return block requested count
	 */
	public int getRequestedCount() {
		int requestedCount = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isRequested() && !blocks[i].isDone())
				++requestedCount;
		}
		return requestedCount;
	}

	/**
	 * Gets a new block to be requested
	 * @return an unrequested block
	 */
	public Block getRequestBlock() {
		for(int i = 0; i < blocks.length; i++) {
			if(!blocks[i].isDone() && !blocks[i].isRequested()) {
				blocks[i].setRequested(true);
				return blocks[i];
			}
		}
		return null;
	}

}
