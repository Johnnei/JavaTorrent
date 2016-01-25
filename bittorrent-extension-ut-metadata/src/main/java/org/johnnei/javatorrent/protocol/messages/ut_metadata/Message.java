package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.io.InvalidObjectException;
import java.util.HashMap;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;

import torrent.encoding.Bencode;
import torrent.encoding.Bencoder;

public abstract class Message implements IMessage {

	protected HashMap<String, Object> dictionary;
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
		try {
			dictionary = decoder.decodeDictionary();
			inStream.moveBack(decoder.remainingChars());
		} catch (InvalidObjectException e) {
		}
	}

	@Override
	public void setReadDuration(int duration) {
	}

}
