package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.util.Map;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.encoding.Bencode;
import org.johnnei.javatorrent.torrent.encoding.Bencoder;

public abstract class Message implements IMessage {

	protected Map<String, Object> dictionary;
	protected String bencodedData;

	public Message() {
		bencodedData = "";
	}

	public Message(int piece) {
		Bencoder encode = new Bencoder();
		encode.dictionaryStart();
		encode.string("msg_type");
		encode.integer(getId());
		encode.string("piece");
		encode.integer(piece);
		encode.dictionaryEnd();
		bencodedData = encode.getBencodedData();
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeString(bencodedData);
	}

	@Override
	public void read(InStream inStream) {
		Bencode decoder = new Bencode(inStream.readString(inStream.available()));
		dictionary = decoder.decodeDictionary();
		inStream.moveBack(decoder.remainingChars());
	}

	@Override
	public void setReadDuration(int duration) {
	}

}
