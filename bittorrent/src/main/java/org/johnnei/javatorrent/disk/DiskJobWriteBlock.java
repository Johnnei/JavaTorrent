package org.johnnei.javatorrent.disk;

import java.io.IOException;
import java.util.function.Consumer;

import org.johnnei.javatorrent.torrent.files.Piece;

public class DiskJobWriteBlock implements IDiskJob {

	private final Consumer<DiskJobWriteBlock> callback;
	private final Piece piece;
	private final int blockIndex;
	private final byte[] data;

	/**
	 * Creates a new job to store a block of a piece.
	 * @param piece The piece in which this block is found
	 * @param blockIndex The index of the block within the given piece.
	 * @param data The bytes to write for the block
	 * @param callback The callback which gets called on completion of this job
	 */
	public DiskJobWriteBlock(Piece piece, int blockIndex, byte[] data, Consumer<DiskJobWriteBlock> callback) {
		this.callback = callback;
		this.piece = piece;
		this.blockIndex = blockIndex;
		this.data = data;
	}

	@Override
	public void process() throws IOException {
		piece.storeBlock(blockIndex, data);
		callback.accept(this);
	}

	/**
	 * Gets the piece for which this store task is being used.
	 * @return The piece for which a block is/was being written.
	 */
	public Piece getPiece() {
		return piece;
	}

	/**
	 * Gets the block index within the owning piece.
	 * @return The index of the block within the piece.
	 *
	 * @see #getPiece()
	 */
	public int getBlockIndex() {
		return blockIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPriority() {
		return CRITICAL;
	}

}
