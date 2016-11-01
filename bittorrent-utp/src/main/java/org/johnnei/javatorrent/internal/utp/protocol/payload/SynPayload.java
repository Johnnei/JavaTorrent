package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

/**
 * The payload which indicates that a connection wants to be established.
 */
public class SynPayload extends AbstractEmptyPayload {

	@Override
	public byte getType() {
		return UtpProtocol.ST_SYN;
	}

	@Override
	public void process(UtpPacket packet, UtpSocketImpl socket) throws IOException {
		if (socket.getConnectionState() != ConnectionState.CONNECTING) {
			// Ignore SYN packets when connected, as defined by spec.
			return;
		}

		// TODO Implement detection of resending and NOT increase the seq_nr on that one.
		socket.send(new UtpPacket(socket, new StatePayload(), true));
	}

	@Override
	public String toString() {
		return "SynPayload[]";
	}
}
