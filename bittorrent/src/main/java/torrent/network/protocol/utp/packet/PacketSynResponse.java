package torrent.network.protocol.utp.packet;


public class PacketSynResponse extends PacketState {
	
	@Override
	protected int getSendSequenceNumber() {
		if(sequenceNumber != -1)
			return sequenceNumber;
		else {
			sequenceNumber = socket.getNextSequenceNumber();
			return sequenceNumber;
		}
	}
}
