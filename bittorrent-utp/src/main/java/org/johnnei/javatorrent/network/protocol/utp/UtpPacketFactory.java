package org.johnnei.javatorrent.network.protocol.utp;

import org.johnnei.javatorrent.network.network.protocol.utp.packet.Packet;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketData;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketFin;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketReset;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketState;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketSyn;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.UtpProtocol;

public class UtpPacketFactory {

	public Packet getFromId(int id) {
		switch (id) {
			case UtpProtocol.ST_DATA:
				return new PacketData();

			case UtpProtocol.ST_FIN:
				return new PacketFin();

			case UtpProtocol.ST_RESET:
				return new PacketReset();

			case UtpProtocol.ST_STATE:
				return new PacketState();

			case UtpProtocol.ST_SYN:
				return new PacketSyn();

			default:
				throw new IllegalArgumentException("Invalid Packet id " + id + " for uTP Protocol");
		}
	}

}
