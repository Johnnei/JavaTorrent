package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;

public class SynPayload extends DatalessPayload {

	@Override
	public void onReceivedPayload(UtpSocket socket) {
		// The sending of the ST_STATE on the ST_SYN is designed to be handled by normal ACK handling system.
	}
}
