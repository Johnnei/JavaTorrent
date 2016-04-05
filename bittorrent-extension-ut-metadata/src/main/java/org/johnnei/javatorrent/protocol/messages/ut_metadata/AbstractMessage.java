package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.util.Collections;
import java.util.Map;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoder;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

public abstract class AbstractMessage implements IMessage {

	protected Map<String, Object> dictionary;
	protected String bencodedData;

	public AbstractMessage() {
		dictionary = Collections.emptyMap();
		bencodedData = "";
	}

	public AbstractMessage(int piece) {
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

}
