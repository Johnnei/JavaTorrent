package torrent.network.protocol.utp.packet;


public class PacketSynResponse extends PacketState {
	
	@Override
	protected int getSendSequenceNumber() {
		return socket.getNextSequenceNumber();
	}
}
