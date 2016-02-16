package org.johnnei.javatorrent.torrent.peer;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Bitfield {

	/**
	 * The lock which prevents thread-unsafety when the bitfield gets resized.<br/>
	 * Read lock: Read/Write data into the {@link #bitfield} (both are allowed as missing one bit is not critical)<br/>
	 * Write lock: Resizing the datastructure
	 */
	private ReadWriteLock resizeLock;
	
	private byte[] bitfield;

	public Bitfield() {
		this(0);
	}

	public Bitfield(int size) {
		bitfield = new byte[size];
		resizeLock = new ReentrantReadWriteLock();
	}

	/**
	 * Increases or decreased the bitfield size but it will preserve the old data
	 * 
	 * @param size The new size to grow/shrink to
	 */
	public void setSize(int size) {
		resizeLock.readLock().lock();
		if (size == bitfield.length) {
			resizeLock.readLock().unlock();
			return;
		}
		resizeLock.readLock().unlock();
		
		resizeLock.writeLock().lock();
		
		byte[] newBitfield = new byte[size];
		System.arraycopy(bitfield, 0, newBitfield, 0, Math.min(size, bitfield.length));
		bitfield = newBitfield;
		
		resizeLock.writeLock().unlock();
	}

	/**
	 * Checks the bitfield if we have the given piece
	 * 
	 * @param pieceIndex the piece to check
	 * @return True if we verified the hash of that piece, else false
	 */
	public boolean hasPiece(int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		
		resizeLock.readLock().lock();
		if (byteIndex < bitfield.length) {
			int bitVal = (0x80 >> bit);
			boolean isSet = (bitfield[byteIndex] & bitVal) > 0;
			resizeLock.readLock().unlock();
			return isSet;
		} else {
			resizeLock.readLock().unlock();
			return false;
		}
	}

	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * 
	 * @param pieceIndex The piece to add
	 */
	public void havePiece(int pieceIndex) {
		havePiece(pieceIndex, false);
	}

	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * 
	 * @param pieceIndex The piece to add
	 * @param mayExpand If the bitfield may grow to fit the new have data
	 */
	public void havePiece(int pieceIndex, boolean mayExpand) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		resizeLock.readLock().lock();
		if (byteIndex >= bitfield.length) {
			if (mayExpand) {
				resizeLock.readLock().unlock();
				setSize(byteIndex + 1);
				resizeLock.readLock().lock();
			} else {
				return; // Prevent IndexOutOfRange
			}
		}
		bitfield[byteIndex] |= (0x80 >> bit);
		resizeLock.readLock().unlock();
	}
	
	/**
	 * Returns a copy of the internal byte array
	 * @return
	 */
	public byte[] getBytes() {
		resizeLock.readLock().lock();
		byte[] clone = bitfield.clone();
		resizeLock.readLock().unlock();
		return clone;
	}

	/**
	 * Goes through the bitfield and checks how many pieces the client has
	 * 
	 * @return The amount of pieces the client has
	 */
	public int countHavePieces() {
		resizeLock.readLock().lock();
		int pieces = bitfield.length * 8;
		int have = 0;
		for (int pieceIndex = 0; pieceIndex < pieces; pieceIndex++) {
			if (hasPiece(pieceIndex)) {
				have++;
			}
		}
		resizeLock.readLock().unlock();
		return have;
	}

}
