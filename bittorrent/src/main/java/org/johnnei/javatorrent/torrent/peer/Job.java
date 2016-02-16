package org.johnnei.javatorrent.torrent.peer;

/**
 * A handler to manage the peer their work queue
 * 
 * @author Johnnei
 * 
 */
public class Job {

	/**
	 * The piece index
	 */
	private int pieceIndex;
	/**
	 * The block index
	 */
	private int block;
	/**
	 * The length of the block<br/>
	 * <b>Optional</b>, Only used with requests from other peers
	 */
	private int length;

	public Job(int pieceIndex) {
		this(pieceIndex, 0);
	}

	public Job(int pieceIndex, int block) {
		this.pieceIndex = pieceIndex;
		this.block = block;
	}

	public Job(int index, int blockIndex, int length) {
		this(index, blockIndex);
		this.length = length;
	}

	public int getPieceIndex() {
		return pieceIndex;
	}

	public int getBlockIndex() {
		return block;
	}

	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pieceIndex;
		result = prime * result + block;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Job other = (Job) obj;
		if (pieceIndex != other.pieceIndex) {
			return false;
		}
		if (block != other.block) {
			return false;
		}
		return true;
	}

}
