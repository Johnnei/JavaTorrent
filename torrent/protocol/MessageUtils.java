package torrent.protocol;

import java.io.IOException;
import java.util.HashMap;

import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.Message;
import torrent.network.Stream;
import torrent.protocol.messages.MessageKeepAlive;

public class MessageUtils {
	
	private static MessageUtils instance;
	
	private MessageUtils() {
		
	}
	
	public static MessageUtils getUtils() {
		return instance;
	}
	
	private HashMap<Integer, IMessage> idToMessage;
	
	public IMessage readMessage(ByteInputStream inStream) throws IOException {
		Stream stream = new Stream(4);
		stream.fill(inStream.readByteArray(4)); //Read Length
		int length = stream.readInt();
		if(length == 0) {
			return new MessageKeepAlive();
		} else {
			stream.fill(inStream.readByteArray(4)); //Read ID
			int id = stream.readInt();
			try {
				IMessage message = idToMessage.get(id).getClass().newInstance();
				stream.fill(inStream.readByteArray(message.getLength() - 1));
				message.read(stream);
				return message;
			} catch (IllegalAccessException | InstantiationException ex ) {
				throw new IOException("Message Read Error", ex);
			}
		}
	}
	
	public void writeMessage(ByteOutputStream outStream, IMessage message) throws IOException {
		Message messageStream = new Message(message.getLength());
		if(message.getLength() > 0) {
			message.write(messageStream.getStream());
		}
		outStream.write(messageStream.getMessage());
	}

}
