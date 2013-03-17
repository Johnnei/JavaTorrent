package torrent.network.protocol.utp;

import java.io.OutputStream;

import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.packet.PacketData;

public class UtpOutputStream extends OutputStream {

	private UtpSocket socket;
	
	public UtpOutputStream(UtpSocket socket) {
		this.socket = socket;
	}
	
	@Override
	public void write(int i) {
		write(new byte[] { (byte)i }, 0, 1);
	}
	
	@Override
	public void write(byte[] array) {
		write(array, 0, array.length);
	}
	
	@Override
	public void write(byte[] array, int offset, int length) {
		int bytesSend = 0;
		while(bytesSend != length) {
			int size = Math.min(socket.getPeerClient().getWindowSize(), length - bytesSend);
			byte[] data = new byte[size];
			System.arraycopy(array, offset + bytesSend, data, 0, size);
			socket.sendPacket(new PacketData(data));
		}
	}

}
