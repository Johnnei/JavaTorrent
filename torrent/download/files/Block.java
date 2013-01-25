package torrent.download.files;

public class Block {

	/**
	 * The index of this block within the piece
	 */
	private int index;
	/**
	 * The size of this block in bytes
	 */
	private int size;
	/**
	 * If this block has been requested
	 */
	private boolean requested;
	/**
	 * If this block has been stored on the hdd
	 */
	private boolean done;

	public Block(int index, int size) {
		this.index = index;
		this.size = size;
	}

	/**
	 * Check if this block has been started
	 * 
	 * @return true if its either requested or done, else false
	 */
	public boolean isStarted() {
		return requested || done;
	}

	public int getSize() {
		return size;
	}

	public boolean isRequested() {
		return requested;
	}

	public void setRequested(boolean requested) {
		this.requested = requested;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public int getIndex() {
		return index;
	}

}
