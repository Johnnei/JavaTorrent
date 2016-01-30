package org.johnnei.javatorrent.network.protocol.utp;

import java.io.OutputStream;

import org.johnnei.javatorrent.network.protocol.UtpSocket;
import org.johnnei.javatorrent.torrent.network.Stream;
import org.johnnei.javatorrent.torrent.network.protocol.utp.packet.PacketData;

public class UtpOutputStream extends OutputStream {

	private UtpSocket socket;
	/**
	 * A buffer to limit the amount of packets send to client when sending single bytes in a row<br/>
	 */
	private Stream buffer;
	
	public UtpOutputStream(UtpSocket socket) {
		this.socket = socket;
		buffer = new Stream(socket.getPacketSize());
	}
	
	@Override
	public void write(int i) {
		if(buffer.writeableSpace() > 0) {
			buffer.writeByte(i);
		} else {
			flush();
			write(i); //We don't want to lose this byte
		}
	}
	
	@Override
	public void write(byte[] array) {
		for(int i = 0; i < array.length; i++) {
			write(array[i]);
		}
	}
	
	@Override
	public void write(byte[] array, int offset, int length) {
		int bytesSend = 0;
		while(bytesSend != length) {
			int size = Math.min(socket.getPacketSize(), length - bytesSend);
			byte[] data = new byte[size];
			System.arraycopy(array, offset + bytesSend, data, 0, size);
			socket.sendPacket(new PacketData(data));
			bytesSend += size;
		}
	}
	
	/**
	 * Forces the stream to send all data to the other-side
	 */
	@Override
	public void flush() {
		if(buffer.getWritePointer() > 0) {
			byte[] data = buffer.getBuffer();
			write(data, 0, buffer.getWritePointer());
			buffer.reset(socket.getPacketSize());
		}
	}

}
