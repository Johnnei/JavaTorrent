package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

/**
 * Payload which indicates that this packet is the final packet in terms of sequence number.
 */
public class FinPayload extends AbstractEmptyPayload {

	@Override
	public byte getType() {
		return UtpProtocol.ST_FIN;
	}

	@Override
	public void process(UtpPacket packet, UtpSocketImpl socket) throws IOException {
		socket.setEndOfStreamSequenceNumber(packet.getSequenceNumber());
		socket.close();
	}

	@Override
	public String toString() {
		return "FinPayload[]";
	}
}
