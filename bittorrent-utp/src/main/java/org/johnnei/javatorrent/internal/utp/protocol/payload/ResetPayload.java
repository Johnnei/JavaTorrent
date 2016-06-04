package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

/**
 * Created by johnn on 23/04/2016.
 */
public class ResetPayload extends AbstractEmptyPayload {

	@Override
	public byte getType() {
		return UtpProtocol.ST_RESET;
	}

	@Override
	public void process(UtpPacket packet, UtpSocketImpl socket) throws IOException {
		socket.onReset();
	}

	@Override
	public String toString() {
		return "ResetPayload[]";
	}
}
