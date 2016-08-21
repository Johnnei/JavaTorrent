package org.johnnei.javatorrent.internal.torrent.peer;

import java.util.Objects;

import org.johnnei.javatorrent.torrent.files.Piece;

/**
 * A handler to manage the peer their work queue
 *
 * @author Johnnei
 *
 */
public class Job {

	private Piece piece;

	/**
	 * The block index
	 */
	private int block;
	/**
	 * The length of the block<br>
	 * <b>Optional</b>, Only used with requests from other peers
	 */
	private int length;

	public Job(Piece piece, int blockIndex, int length) {
		this.piece = Objects.requireNonNull(piece);
		this.block = blockIndex;
		this.length = length;
	}

	public Piece getPiece() {
		return piece;
	}

	public int getBlockIndex() {
		return block;
	}

	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		return Objects.hash(piece, block, length);
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
		if (!Objects.equals(piece, other.piece)) {
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
		return String.format("Job[piece=%s, block=%d, length=%d]", piece, block, length);
	}

}
