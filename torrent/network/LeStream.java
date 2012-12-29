package torrent.network;

/**
 * Implementation of Stream class but then in Little Endian formats instead of Big Endian
 * @author Johnnei
 *
 */
public class LeStream extends Stream {
	
	/**
	 * Flip the last x bytes
	 * @param amount
	 * The amount of bytes to flip
	 */
	private void flip(int amount) {
		flip(offset - amount, amount);
	}
	
	/**
	 * Flips a section of the buffer from BE to LE
	 * @param offset
	 * The offset to start at
	 * @param amount
	 * The amount of bytes to swap
	 */
	private void flip(int offset, int amount) {
		byte[] tempBuffer = new byte[amount];
		for(int i = 0; i < amount; i++) {
			tempBuffer[i] = buffer[i + offset];
		}
		for(int i = 0; i < amount; i++) {
			buffer[offset + i] = tempBuffer[amount - i - 1];
		}
	}
	
	@Override
	public void writeInt(int i) {
		super.writeInt(i);
		flip(4);
	}
	
	@Override
	public void writeLong(long l) {
		super.writeLong(l);
		flip(8);
	}
	
	@Override
	public int readInt() {
		flip(offset, 4);
		return super.readInt();
	}
	
	@Override
	public long readLong() {
		flip(offset, 8);
		return super.readLong();
	}
}
