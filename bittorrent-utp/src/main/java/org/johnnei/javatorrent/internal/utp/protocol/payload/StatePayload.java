package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

/**
 * The state message which indicated that a previous packet has correctly been received.
 */
public class StatePayload extends AbstractEmptyPayload {

	@Override
	public byte getType() {
		return UtpProtocol.ST_STATE;
	}

	@Override
	public void process(UtpPacket packet, UtpSocketImpl socket) {
		// The processing of ACK is done in UtpSocketImpl itself.
	}

	@Override
	public String toString() {
		return "StatePayload[]";
	}
}
