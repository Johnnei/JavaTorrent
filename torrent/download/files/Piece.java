package torrent.download.files;

import torrent.download.Torrent;
import torrent.util.ISortable;

public class Piece extends PieceInfo implements ISortable {

	/**
	 * The pieces used to reference to any smaller sub-piece
	 */
	private Block[] blocks;
	private byte[] data;
	private int obtainedData;
	private boolean obtainedAllData;
	private boolean isRequested;

	public Piece(int index, int size) {
		super(index, size);
		data = new byte[size];
		obtainedAllData = false;
		obtainedData = 0;
	}

	public boolean isStarted() {
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isRequested())
				return true;
		}
		return false;
	}

	public int getProgress() {
		double done = 0D;
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isDone())
				++done;
		}
		int p = (int) (100 * (done / blocks.length));
		return p;
	}

	public void setRequested(boolean requested) {
		isRequested = requested;
	}

	public boolean isRequested() {
		return isRequested;
	}

	public boolean isRequestedAll() {
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isRequested())
				return false;
		}
		return true;
	}

	public boolean hasData() {
		return obtainedAllData;
	}

	public boolean hasAllBlocks() {
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isDone())
				return false;
		}
		return true;
	}

	public boolean fill(byte[] data, int offset) {
		if (offset + data.length <= getSize()) {
			for (int i = 0; i < data.length; i++) {
				this.data[offset + i] = data[i];
				obtainedData++;
			}
			blocks[offset / Torrent.REQUEST_SIZE].setDone(true);
			obtainedAllData = obtainedData == this.data.length;
		}
		return obtainedAllData;
	}

	/**
	 * Resets the buffer to store new bytes
	 */
	public void resetBuffer() {
		data = new byte[getSize()];
		obtainedAllData = false;
	}

	public void reset() {
		data = new byte[0];
		obtainedAllData = false;
		for (int i = 0; i < blocks.length; i++) {
			blocks[i].setDone(false);
			blocks[i].setRequested(false);
		}
	}

	public void cancel(int piece) {
		blocks[piece].setRequested(false);
	}

	public boolean fill(byte[] data) {
		if (data.length == getSize()) {
			this.data = data;
			obtainedData = data.length;
			obtainedAllData = true;
		}
		return obtainedAllData;
	}

	/**
	 * Fills the message with the request data
	 * 
	 * @param message
	 *            The message to be filled
	 * @return An array of size 3 in format { blockId, length, Offset } Or an array of size 0 in case of an error
	 */
	public int[] getPieceRequest() {
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i].isRequested())
				continue;
			blocks[i].setRequested(true);
			return new int[] { i, blocks[i].getSize(), Torrent.REQUEST_SIZE * blocks[i].getIndex() };
		}
		return new int[] {};
	}

	public void setSize(int size) {
		super.setSize(size);
		int blockCount = (int) Math.ceil(getSize() / Torrent.REQUEST_SIZE);
		blocks = new Block[blockCount];
		int remaining = getSize();
		for (int i = 0; i < blockCount; i++) {
			int pSize = (remaining >= Torrent.REQUEST_SIZE) ? Torrent.REQUEST_SIZE : remaining;
			blocks[i] = new Block(i, pSize);
			remaining -= blocks[i].getSize();
		}
	}

	public byte[] getData() {
		return data;
	}

	public int getBlockCount() {
		return blocks.length;
	}

	public int getDoneCount() {
		int count = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isDone())
				count++;
		}
		return count;
	}

	public int getRequestedCount() {
		int count = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isRequested() && !blocks[i].isDone())
				count++;
		}
		return count;
	}

	@Override
	public int getValue() {
		int count = 0;
		for(int i = 0; i < blocks.length; i++) {
			if(blocks[i].isRequested())
				count++;
			if(blocks[i].isDone()) //Done counts for 2
				count++;
		}
		return count;
	}

}
