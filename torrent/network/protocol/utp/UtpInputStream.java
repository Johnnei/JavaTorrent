package torrent.network.protocol.utp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import torrent.network.Stream;
import torrent.network.protocol.utp.packet.PacketData;

public class UtpInputStream extends InputStream {

	/**
	 * The buffer of unread data
	 */
	private Stream buffer;
	/**
	 * The queue of data which has arrived in the wrong order
	 */
	private ArrayList<PacketData> dataQueue;
	/**
	 * The last sequence number which was added to the {@link #buffer}
	 */
	private int lastSequenceNumber;
	
	public UtpInputStream() {
		buffer = new Stream(5120); //5kB buffer
		dataQueue = new ArrayList<>();
		lastSequenceNumber = 1;
	}
	
	/**
	 * Processes the {@link PacketData} into the correct order in the inputStream
	 * @param packet
	 */
	public void receiveData(PacketData packet) {
		if(packet.getSequenceNumber() == (lastSequenceNumber + 1)) {
			//This packet is the next in-chain
			byte[] data = packet.getData();
			if(buffer.writeableSpace() >= data.length) {
				buffer.writeByte(data);
			} else {
				buffer.refit();
				if(buffer.writeableSpace() >= data.length) {
					buffer.writeByte(data);
				} else {
					buffer.expand(data.length - buffer.writeableSpace());
					buffer.writeByte(data);
				}
			}
			lastSequenceNumber = packet.getSequenceNumber();
			//Check if the queue contains more packet after this one
			if(dataQueue.size() > 0) {
				for(int i = 0; i < dataQueue.size(); i++) {
					PacketData dataPacket = dataQueue.get(i);
					if(dataPacket.getSequenceNumber() == (lastSequenceNumber + 1)) {
						dataQueue.remove(i);
						receiveData(packet);
						return;
					}
				}
			}
		} else {
			dataQueue.add(packet);
		}
	}

	@Override
	public int read() throws IOException {
		if(buffer.available() > 0) {
			return buffer.readByte() & 0xFF;
		} else
			return -1;
	}

}
