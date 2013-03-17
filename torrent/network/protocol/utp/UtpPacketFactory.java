package torrent.network.protocol.utp;

import java.util.HashMap;

import torrent.network.protocol.utp.packet.Packet;
import torrent.network.protocol.utp.packet.PacketData;
import torrent.network.protocol.utp.packet.PacketFin;
import torrent.network.protocol.utp.packet.PacketReset;
import torrent.network.protocol.utp.packet.PacketState;
import torrent.network.protocol.utp.packet.PacketSyn;

public class UtpPacketFactory {
	private HashMap<Integer, Packet> idToPacket;

	public UtpPacketFactory() {
		idToPacket = new HashMap<>();
		register(new PacketSyn());
		register(new PacketData());
		register(new PacketReset());
		register(new PacketState());
		register(new PacketFin());
	}
	
	private void register(Packet p) {
		idToPacket.put(p.getId(), p);
	}
	
	public Packet getFromId(int id) {
		Packet p = idToPacket.get(id);
		if(p == null)
			throw new IllegalArgumentException("Invalid Packet id " + id + " for uTP Protocol");
		else {
			try {
				return p.getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalAccessError(p.getClass().getName() + " has no default-constructor! Fix it!");
			}
		}
	}

}
