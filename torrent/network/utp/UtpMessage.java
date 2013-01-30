package torrent.network.utp;

import torrent.network.Stream;

/**
 * As UtpMessage are actual packets with their own ID's it can't be recalculated.
 * @author Johnnei
 *
 */
public class UtpMessage {

	/**
	 * The data send for this message including headers
	 */
	private byte[] data;
	/**
	 * The sequence number which belongs to this message
	 */
	private int seq_nr;
	
	public UtpMessage(UtpSocket socket, int type, int seq_nr, int ack_nr) {
		this.seq_nr = seq_nr;
		Stream dataStream = new Stream(20);
		dataStream.writeByte((byte)(type << 4 | UtpSocket.VERSION)); //Type and Version
		dataStream.writeByte(0); //No Extension
		dataStream.writeShort(socket.getConnectionId());
		dataStream.writeLong(0L); //timestamp, Will be set as close to the actual send time
		dataStream.writeLong(0L); //timestamp_diff, Will be set to the value we know once it will be send
		dataStream.writeLong(socket.getWindowSize());
		dataStream.writeInt(seq_nr);
		dataStream.writeInt(ack_nr);
		data = dataStream.getBuffer();
	}
	
	/**
	 * Updates the timestamp information to make the information as accurate as possible
	 * 
	 * @param socket The socket to copy the timestamp_diff from
	 */
	public void setTimestamp(UtpSocket socket) {
		Stream dataStream = new Stream(0);
		dataStream.fill(data);
		dataStream.skipWrite(-data.length); //New offset: 0
		dataStream.skipWrite(4);
		dataStream.writeLong(getCurrentMicroseconds());
		dataStream.writeLong(socket.getDelay());
		data = dataStream.getBuffer();
	}
	
	/**
	 * Gets the current microseconds<br/>
	 * The problem is that java does not have a precise enough system so it's just the currentMillis * 1000<br/>
	 * But there is nanoSeconds bla bla, Read the javadocs please
	 * 
	 * @return
	 */
	private long getCurrentMicroseconds() {
		return System.currentTimeMillis() * 1000;
	}
	
	@Override
	public int hashCode() {
		return seq_nr;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof UtpMessage) {
			UtpMessage m = (UtpMessage)o;
			return m.hashCode() == hashCode();
		}
		return false;
	}

}
