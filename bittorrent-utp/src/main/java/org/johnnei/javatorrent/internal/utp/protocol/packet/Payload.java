package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public interface Payload {

	/**
	 * Updates the socket state based on the received payload (this instance).
	 *
	 * @param header The header which was sent along with this payload.
	 * @param socket The socket which received the payload.
	 */
	void onReceivedPayload(UtpHeader header, UtpSocket socket);

	/**
	 * @return The data which is associated with this payload.
	 */
	byte[] getData();

	/**
	 * @return The type of the packet.
	 */
	PacketType getType();

}
