package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * The definition of the different payloads which are defined by the 'type' field of the {@link UtpPacket}
 */
public interface IPayload {

	/**
	 * Gets the type of this payload as defined in {@link org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol}
	 * @return
	 */
	byte getType();

	/**
	 * Reads the payload. This is called
	 * @param inStream
	 */
	void read(InStream inStream);

	/**
	 * Writes the payload. This is called after the header has been written.
	 * @param outStream The stream to write on.
	 */
	void write(OutStream outStream);

	/**
	 * Process the packet.
	 * @param packet The packet header associated to this payload.
	 * @param socket The socket which received the packet.
	 */
	void process(UtpPacket packet, UtpSocketImpl socket) throws IOException;

	/**
	 * Gets the payload size in bytes.
	 * @return The size in bytes.
	 */
	int getSize();
}
