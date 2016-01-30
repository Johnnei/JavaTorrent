package org.johnnei.javatorrent.torrent.protocol;

import java.io.IOException;

import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.network.ByteOutputStream;

public class MessageUtils {

	private static MessageUtils instance = new MessageUtils();

	public static MessageUtils getUtils() {
		return instance;
	}

	public void writeMessage(ByteOutputStream outStream, IMessage message) throws IOException {
		OutStream outBuffer = new OutStream(message.getLength() + 4);
		outBuffer.writeInt(message.getLength());

		if (message.getLength() > 0) {
			outBuffer.writeByte(message.getId());
			message.write(outBuffer);
		}

		outStream.write(outBuffer.toByteArray());
	}

}
