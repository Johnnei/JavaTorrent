package torrent.network;

public class Message {

	private Stream stream;

	public Message(int length) {
		stream = new Stream(length + 4);
		stream.writeInt(length);
	}

	public Stream getStream() {
		return stream;
	}

	public byte[] getMessage() {
		return stream.getBuffer();
	}

	public byte getMessageId() {
		return stream.getBuffer()[5];
	}

}
