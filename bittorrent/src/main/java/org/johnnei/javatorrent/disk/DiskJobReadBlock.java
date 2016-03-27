package org.johnnei.javatorrent.disk;

import java.io.IOException;
import java.util.function.Consumer;

import org.johnnei.javatorrent.torrent.files.Piece;

public class DiskJobReadBlock implements IDiskJob {

	private final Consumer<DiskJobReadBlock> callback;
	private final Piece piece;
	private final int offset;

	private final int length;

	private byte[] blockData;

	public DiskJobReadBlock(Piece piece, int offset, int length, Consumer<DiskJobReadBlock> callback) {
		this.callback = callback;
		this.piece = piece;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process() throws IOException {
		blockData = piece.loadPiece(offset, length);
		callback.accept(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPriority() {
		return NORMAL;
	}

	/**
	 * The read data from the piece
	 * @return The read data
	 * @see #getPiece()
	 */
	public byte[] getBlockData() {
		return blockData;
	}

	/**
	 * The piece from which data was read.
	 * @return The piece from which data was read.
	 */
	public Piece getPiece() {
		return piece;
	}

	/**
	 * Gets the offset at which the data started reading.
	 * @return The data offset
	 */
	public int getOffset() {
		return offset;
	}
}
