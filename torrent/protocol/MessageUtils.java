package torrent.protocol;

import java.io.IOException;
import java.util.HashMap;

import torrent.download.peer.Peer;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.Message;
import torrent.network.Stream;
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

	public boolean canReadMessage(ByteInputStream inStream, Peer p) throws IOException {
		if (inStream.getBuffer() == null) {
			if (inStream.available() >= 4) {
				inStream.initialiseBuffer();
				inStream.getBuffer().fill(inStream.readByteArray(4));
				inStream.getBuffer().resetReadPointer();
				int length = inStream.getBuffer().readInt();
				inStream.getBuffer().expand(length);
			}
		}
		Stream buffer = inStream.getBuffer();
		if (buffer != null) {
			if (inStream.available() > 0) {
				int readAmount = Math.min(inStream.available(), buffer.getBuffer().length - buffer.getWritePointer());
				buffer.writeByte(inStream.readByteArray(readAmount));
			}
			p.setStatus("Receiving Message of length: " + (buffer.getWritePointer() - 4) + "/" + (buffer.getBuffer().length - 4));
			return buffer.getWritePointer() == buffer.getBuffer().length;
		}
		return false;
	}

	public IMessage readMessage(ByteInputStream inStream, Peer p) throws IOException {
		Stream stream = inStream.getBuffer();
		int duration = inStream.getBufferLifetime();
		stream.resetReadPointer();
		int length = stream.readInt();
		if (length == 0) {
			p.setStatus("Received Message: KeepAlive");
			inStream.resetBuffer();
			return new MessageKeepAlive();
		} else {
			int id = stream.readByte();
			try {
				if (!idToMessage.containsKey(id))
					throw new IOException("Unhandled Message: " + id);
				IMessage message = idToMessage.get(id).getClass().newInstance();
				p.setStatus("Received Message: " + message.toString());
				message.setReadDuration(duration);
				message.read(stream);
				inStream.resetBuffer();
				return message;
			} catch (IllegalAccessException | InstantiationException ex) {
				throw new IOException("Message Read Error", ex);
			}
		}
	}

	public void writeMessage(ByteOutputStream outStream, IMessage message) throws IOException {
		Message messageStream = new Message(message.getLength());
		if (message.getLength() > 0) {
			messageStream.getStream().writeByte(message.getId());
			message.write(messageStream.getStream());
		}
		outStream.write(messageStream.getMessage());
	}

}
