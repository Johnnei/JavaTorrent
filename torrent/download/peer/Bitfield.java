package torrent.download.peer;


public class Bitfield {

	private byte[] bitfield;

	public Bitfield() {
		this(0);
	}

	public Bitfield(int size) {
		bitfield = new byte[size];
	}

	/**
	 * Increases or decreased the bitfield size but it will preserve the old data
	 * 
	 * @param size The new size to grow/shrink to
	 */
	public void setSize(int size) {
		if (size == bitfield.length) {
			return;
		}
		
		byte[] newBitfield = new byte[size];
		System.arraycopy(bitfield, 0, newBitfield, 0, Math.min(size, bitfield.length));
		bitfield = newBitfield;
	}

	/**
	 * Checks the bitfield if we have the given piece
	 * 
	 * @param pieceIndex the piece to check
	 * @return True if we verified the hash of that piece, else false
	 */
	public boolean hasPiece(int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		if (byteIndex < bitfield.length) {
			int bitVal = (0x80 >> bit);
			return (bitfield[byteIndex] & bitVal) > 0;
		} else {
			return false;
		}
	}

	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * 
	 * @param pieceIndex The piece to add
	 */
	public void havePiece(int pieceIndex) {
		havePiece(pieceIndex, false);
	}

	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * 
	 * @param pieceIndex The piece to add
	 * @param mayExpand If the bitfield may grow to fit the new have data
	 */
	public void havePiece(int pieceIndex, boolean mayExpand) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		if (bitfield.length < byteIndex) {
			if (mayExpand) {
				byte[] newBitfield = new byte[byteIndex + 1];
				for (int i = 0; i < bitfield.length; i++) {
					newBitfield[i] = bitfield[i];
				}
				this.bitfield = newBitfield;
			} else {
				return; // Prevent IndexOutOfRange
			}
		}
		bitfield[byteIndex] |= (0x80 >> bit);
	}
	
	/**
	 * Returns a copy of the internal byte array
	 * @return
	 */
	public byte[] getBytes() {
		return bitfield.clone();
	}

	/**
	 * Goes through the bitfield and checks how many pieces the client has
	 * 
	 * @return The amount of pieces the client has
	 */
	public int countHavePieces() {
		int pieces = bitfield.length * 8;
		int have = 0;
		for (int pieceIndex = 0; pieceIndex < pieces; pieceIndex++) {
			if (hasPiece(pieceIndex)) {
				have++;
			}
		}
		return have;
	}

}
