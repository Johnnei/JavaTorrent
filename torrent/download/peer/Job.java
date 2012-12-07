package torrent.download.peer;

/**
 * A handler to manage the peer their work queue
 * @author Johnnei
 *
 */
public class Job {
	
	private int pieceIndex;
	private int subPiece;
	
	public Job(int pieceIndex) {
		this(pieceIndex, 0);
	}
	
	public Job(int pieceIndex, int subPiece) {
		this.pieceIndex = pieceIndex;
		this.subPiece = subPiece;
	}
	
	public int getPieceIndex() {
		return pieceIndex;
	}
	
	public int getSubpiece() {
		return subPiece;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pieceIndex;
		result = prime * result + subPiece;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Job other = (Job) obj;
		if (pieceIndex != other.pieceIndex) {
			return false;
		}
		if (subPiece != other.subPiece) {
			return false;
		}
		return true;
	}

}
