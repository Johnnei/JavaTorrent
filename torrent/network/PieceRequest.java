package torrent.network;

public class PieceRequest {

	private int index;
	private int offset;
	private int length;

	public PieceRequest(int index, int offset, int length) {
		this.index = index;
		this.offset = offset;
		this.length = length;
	}

	public int getIndex() {
		return index;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

}
