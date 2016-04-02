package org.johnnei.javatorrent.internal.torrent.peer;

import java.util.Objects;

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
		return Objects.hash(pieceIndex, block, length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Job)) {
			return false;
		}
		Job other = (Job) obj;
		if (pieceIndex != other.pieceIndex) {
			return false;
		}
		if (block != other.block) {
			return false;
		}

		if (length != other.length) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format("Job[piece=%d, block=%d, length=%d]", pieceIndex, block, length);
	}

}
