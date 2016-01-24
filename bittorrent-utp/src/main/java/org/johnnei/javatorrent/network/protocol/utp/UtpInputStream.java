package org.johnnei.javatorrent.network.protocol.utp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.johnnei.javatorrent.torrent.network.protocol.utp.packet.PacketData;

import torrent.network.Stream;

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
	private boolean isFirstPacket;
	
	public UtpInputStream() {
		buffer = new Stream(5120); //5kB buffer
		dataQueue = new ArrayList<>();
		lastSequenceNumber = 1;
		isFirstPacket = true;
	}
	
	/**
	 * Processes the {@link PacketData} into the correct order in the inputStream
	 * @param packet
	 */
	public void receiveData(PacketData packet) {
		System.err.println("XXXXX| Received Data SeqNr: " + packet.getSequenceNumber() + " (" + packet.getSize() + " bytes)");
		if(isFirstPacket) {
			lastSequenceNumber = packet.getSequenceNumber() - 1;
			isFirstPacket = false;
		}
		if(packet.getSequenceNumber() == (lastSequenceNumber + 1)) {
			//This packet is the next in-chain
			System.out.println("Data is in sequence");
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
			System.out.println("Missing a packet");
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
	
	@Override
	public int available() {
		return buffer.available();
	}

	/**
	 * Sets the first sequence number
	 * @param sequenceNumber
	 */
	public void setSequenceNumber(int sequenceNumber) {
		lastSequenceNumber = sequenceNumber;
	}

}
