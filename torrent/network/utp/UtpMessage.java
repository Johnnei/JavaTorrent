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
	
	/**
	 * Creates a full uTP Message which can be used for all non-data messages
	 * @param connectionId The connectionId we want to send along
	 * @param windowSize The window_size which we wan't to advertise
	 * @param type The type of this message
	 * @param seq_nr The sequence number of this message
	 * @param ack_nr The sequence number which we want to acknowledge
	 */
	public UtpMessage(int connectionId, int windowSize, int type, int seq_nr, int ack_nr) {
		this.seq_nr = seq_nr;
		Stream dataStream = new Stream(20);
		dataStream.writeByte((byte)(type << 4 | UtpSocket.VERSION)); //Type and Version
		dataStream.writeByte(0); //No Extension
		dataStream.writeShort(connectionId);
		dataStream.writeInt(0L); //timestamp, Will be set as close to the actual send time
		dataStream.writeInt(0L); //timestamp_diff, Will be set to the value we know once it will be send
		dataStream.writeInt(windowSize);
		dataStream.writeShort(seq_nr);
		dataStream.writeShort(ack_nr);
		data = dataStream.getBuffer();
	}
	
	/**
	 * Creates a full uTP Message which can be used for a data messages
	 * @param socket The socket to read the connectionId and Windowsize from
	 * @param type The type of this message
	 * @param seq_nr The sequence number of this message
	 * @param ack_nr The sequence number which we want to acknowledge
	 * @param data The data which will be put after the header
	 */
	public UtpMessage(UtpSocket socket, int type, int seq_nr, int ack_nr, byte[] data) {
		this(socket, type, seq_nr, ack_nr);
		Stream dataStream = new Stream(this.data);
		dataStream.expand(data.length);
		dataStream.writeByte(data);
		this.data = dataStream.getBuffer();
	}
	
	/**
	 * Creates a full uTP Message which can be used for all non-data messages
	 * @param socket The socket to read the connectionId and Windowsize from
	 * @param type The type of this message
	 * @param seq_nr The sequence number of this message
	 * @param ack_nr The sequence number which we want to acknowledge
	 */
	public UtpMessage(UtpSocket socket, int type, int seq_nr, int ack_nr) {
		this(socket.getMyClient().getConnectionId(), socket.getMyClient().getWindowSize(), type, seq_nr, ack_nr);
	}
	
	/**
	 * Creates a sample message used to manage the lists of Messages
	 * @param seq_nr The sequence number of this message
	 */
	public UtpMessage(int seq_nr) {
		this.seq_nr = seq_nr;
	}
	
	/**
	 * Updates the timestamp information to make the information as accurate as possible
	 * 
	 * @param socket The socket to copy the timestamp_diff from
	 */
	public void setTimestamp(UtpSocket socket) {
		Stream dataStream = new Stream(data);
		dataStream.resetWritePointer();
		dataStream.skipWrite(4);
		dataStream.writeInt(socket.getCurrentMicroseconds());
		dataStream.writeInt(socket.getPeerClient().getDelay());
		data = dataStream.getBuffer();
	}
	
	/**
	 * Gets the size of the message in bytes
	 * @return The amount of bytes used for this message
	 */
	public int getSize() {
		return data.length;
	}
	
	/**
	 * Gets the send time in miliseconds
	 * @return the time this mesasge has been send
	 */
	public int getSendTime() {
		Stream dataStream = new Stream(data);
		dataStream.readInt(); //Skip 4 bytes
		return dataStream.readInt();
	}
	
	public int getType() {
		return data[0] >>> 4;
	}
	
	/**
	 * The data to send for this message
	 * @return
	 */
	public byte[] getData() {
		return data;
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
