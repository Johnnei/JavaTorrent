package torrent.download.files;

import torrent.download.Torrent;
import torrent.network.Message;

public class Piece extends PieceInfo {

	/**
	 * The pieces used to reference to any smaller sub-piece
	 */
	private SubPiece[] subPieces;
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
		for (int i = 0; i < subPieces.length; i++) {
			if (subPieces[i].isRequested())
				return true;
		}
		return false;
	}

	public int getProgress() {
		double done = 0D;
		for (int i = 0; i < subPieces.length; i++) {
			if (subPieces[i].isDone())
				++done;
		}
		int p = (int) (100 * (done / subPieces.length));
		return p;
	}

	public void setRequested(boolean requested) {
		isRequested = requested;
	}

	public boolean isRequested() {
		return isRequested;
	}

	public boolean isRequestedAll() {
		for (int i = 0; i < subPieces.length; i++) {
			if (!subPieces[i].isRequested())
				return false;
		}
		return true;
	}

	public boolean hasData() {
		return obtainedAllData;
	}

	public boolean hasAllSubpieces() {
		for (int i = 0; i < subPieces.length; i++) {
			if (!subPieces[i].isDone())
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
			subPieces[offset / Torrent.REQUEST_SIZE].setDone(true);
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
		for (int i = 0; i < subPieces.length; i++) {
			subPieces[i].setDone(false);
			subPieces[i].setRequested(false);
		}
	}

	public void cancel(int piece) {
		subPieces[piece].setRequested(false);
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
	 * @return An array of size 2 in format { pieceId, subPieceId } Or an array of size 0 in case of an error
	 */
	public int[] fillPieceRequest(Message message) {
		for (int i = 0; i < subPieces.length; i++) {
			if (subPieces[i].isRequested())
				continue;
			message.getStream().writeInt(getIndex());
			message.getStream().writeInt(Torrent.REQUEST_SIZE * subPieces[i].getIndex()); // offset
			message.getStream().writeInt(subPieces[i].getSize()); // size
			message.getStream().fit();
			subPieces[i].setRequested(true);
			return new int[] { i, subPieces[i].getSize() };
		}
		return new int[] {};
	}

	public void setSize(int size) {
		super.setSize(size);
		int subPieceCount = (int) Math.ceil(getSize() / Torrent.REQUEST_SIZE);
		subPieces = new SubPiece[subPieceCount];
		int remaining = getSize();
		for (int i = 0; i < subPieceCount; i++) {
			int pSize = (remaining >= Torrent.REQUEST_SIZE) ? Torrent.REQUEST_SIZE : remaining;
			subPieces[i] = new SubPiece(i, pSize);
			remaining -= subPieces[i].getSize();
		}
	}

	public byte[] getData() {
		return data;
	}

	public int getSubpieceCount() {
		return subPieces.length;
	}

	public int getDoneCount() {
		int count = 0;
		for(int i = 0; i < subPieces.length; i++) {
			if(subPieces[i].isDone())
				count++;
		}
		return count;
	}

	public int getRequestedCount() {
		int count = 0;
		for(int i = 0; i < subPieces.length; i++) {
			if(subPieces[i].isRequested() && !subPieces[i].isDone())
				count++;
		}
		return count;
	}

}
