package torrent.download.files;

import torrent.encoding.SHA1;

/**
 * A commenly more large piece with a 20 byte SHA1 hash
 * 
 * @author Johnnei
 * 
 */
public class HashedPiece extends Piece {

	private byte[] sha1Hash;

	public HashedPiece(int index, int size, byte[] sha1Hash) {
		super(index, 0);
		this.sha1Hash = sha1Hash;
		setSize(size);
	}

	/**
	 * Checks if the hash matches with the one in the torrent file
	 * 
	 * @return
	 */
	public boolean checkHash() {
		return SHA1.match(sha1Hash, SHA1.hash(getData()));
	}
}
