package torrent.protocol;

import java.io.IOException;
import java.util.HashMap;

import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.InStream;
import torrent.network.OutStream;
import torrent.protocol.messages.MessageBitfield;
import torrent.protocol.messages.MessageBlock;
import torrent.protocol.messages.MessageCancel;
import torrent.protocol.messages.MessageChoke;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.MessageInterested;
import torrent.protocol.messages.MessageKeepAlive;
import torrent.protocol.messages.MessageRequest;
import torrent.protocol.messages.MessageUnchoke;
import torrent.protocol.messages.MessageUninterested;
import torrent.protocol.messages.extension.MessageExtension;

public class MessageUtils {

	private static MessageUtils instance = new MessageUtils();

	private MessageUtils() {
		idToMessage = new HashMap<>();
		registerMessage(new MessageChoke());
		registerMessage(new MessageUnchoke());
		registerMessage(new MessageInterested());
		registerMessage(new MessageUninterested());
		registerMessage(new MessageHave());
		registerMessage(new MessageBitfield());
		registerMessage(new MessageRequest());
		registerMessage(new MessageBlock());
		registerMessage(new MessageExtension());
		registerMessage(new MessageCancel());
	}

	private void registerMessage(IMessage message) {
		idToMessage.put(message.getId(), message);
	}

	public static MessageUtils getUtils() {
		return instance;
	}

	private HashMap<Integer, IMessage> idToMessage;

	public IMessage readMessage(ByteInputStream inStream) throws IOException {
		InStream stream = inStream.getBufferedMessage();
		int duration = inStream.getBufferLifetime();
		int length = stream.readInt();
		if (length == 0) {
			return new MessageKeepAlive();
		} else {
			int id = stream.readByte();
			try {
				if (!idToMessage.containsKey(id))
					throw new IOException("Unhandled Message: " + id);
				IMessage message = idToMessage.get(id).getClass().newInstance();
				message.setReadDuration(duration);
				message.read(stream);
				return message;
			} catch (IllegalAccessException | InstantiationException ex) {
				throw new IOException("Message Read Error", ex);
			}
		}
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
