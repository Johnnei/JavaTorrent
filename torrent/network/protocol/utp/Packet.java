package torrent.network.protocol.utp;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

public abstract class Packet {
	
	private PacketHeader header;
	
	public Packet() {
		header = new PacketHeader();
	}
	
	public Packet(PacketHeader header) {
		this.header = header;
	}
	
	/**
	 * Writes the message to the outputstream
	 * @param outStream
	 */
	public void write(Stream outStream) {
		header.write(outStream);
		writePacket(outStream);
	}
	
	/**
	 * Reads the message from the inputstream
	 * @param inStream
	 */
	public void read(Stream inStream) {
		header.read(inStream);
		readPacket(inStream);
	}
	
	/**
	 * Writes the message to the output stream
	 * 
	 * @param outStream
	 */
	protected abstract void writePacket(Stream outStream);

	/***
	 * Read a message from the inputStream
	 * 
	 * @param inStream
	 */
	protected abstract void readPacket(Stream inStream);

	/**
	 * Process the message
	 * 
	 * @param peer The client which should process this message
	 */
	public abstract void process(Peer peer, UtpSocket socket);
	
	public abstract void getId();


}
