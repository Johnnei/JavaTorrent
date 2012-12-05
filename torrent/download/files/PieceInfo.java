package torrent.download.files;

public class PieceInfo {

	private int index;
	private int size;

	public PieceInfo(int index, int size) {
		this.index = index;
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	/**
	 * Used to avoid OutOfMemory Exceptions
	 * 
	 * @param size
	 * @return
	 */
	public void setSize(int size) {
		this.size = size;
	}

	public int getIndex() {
		return index;
	}

}
