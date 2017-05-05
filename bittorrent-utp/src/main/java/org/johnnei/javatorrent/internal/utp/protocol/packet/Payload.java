package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public interface Payload {

	/**
	 * Updates the socket state based on the received payload (this instance).
	 *
	 * @param socket The socket which received the payload.
	 */
	void onReceivedPayload(UtpSocket socket);

	/**
	 * @return The data which is associated with this payload.
	 */
	byte[] getData();

	/**
	 * @return The type of the packet.
	 */
	PacketType getType();

}
