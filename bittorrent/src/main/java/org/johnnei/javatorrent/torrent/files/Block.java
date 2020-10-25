package org.johnnei.javatorrent.torrent.files;

public final class Block {

	public static Block copyWithStatus(Block original, BlockStatus newStatus) {
		return new Block(original.index, original.size, newStatus);
	}

	/**
	 * The index of this block within the piece
	 */
	private final int index;

	/**
	 * The size of this block in bytes
	 */
	private final int size;

	/**
	 * The status of this block
	 */
	private BlockStatus status;

	/**
	 * Creates a new block which is part of a {@link Piece}.
	 * @param index The index within the owning piece.
	 * @param size The size in bytes of this block.
	 */
	public Block(int index, int size) {
		this(index, size, BlockStatus.Needed);
	}

	private Block(int index, int size, BlockStatus status) {
		this.index = index;
		this.size = size;
		this.status = status;
	}

	/**
	 * Updates the status of the block.
	 * @param status The new status of this block.
	 * @deprecated Blocks are moving to become immutable use the copy factory {@link #copyWithStatus(Block, BlockStatus)} instead.
	 */
	@Deprecated
	public void setStatus(BlockStatus status) {
		this.status = status;
	}

	/**
	 * Gets the current status of this block.
	 * @return The status of this block.
	 */
	public BlockStatus getStatus() {
		return status;
	}

	/**
	 * Gets the amount of bytes expected for this block.
	 * @return The amount of bytes for this block.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gets the index of this block within the owning {@link Piece}
	 * @return The index of this block.
	 */
	public int getIndex() {
		return index;
	}

	public String toString() {
		return String.format("Block[index=%d, size=%d, status=%s]", index, size, status);
	}

}
