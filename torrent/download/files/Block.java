package torrent.download.files;

public class Block extends PieceInfo {

	private boolean requested;
	private boolean done;

	public Block(int index, int size) {
		super(index, size);
		requested = false;
		done = false;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isRequested() {
		return requested;
	}

	public void setDone(boolean b) {
		done = b;
	}

	public void setRequested(boolean b) {
		requested = b;
	}

}
