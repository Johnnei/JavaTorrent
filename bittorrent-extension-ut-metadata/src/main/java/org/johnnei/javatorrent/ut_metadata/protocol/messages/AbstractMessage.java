package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.util.Collections;
import java.util.Map;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoder;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

public abstract class AbstractMessage implements IMessage {

	protected static final String PIECE_KEY = "piece";
	protected Map<String, Object> dictionary;
	protected String bencodedData;

	public AbstractMessage() {
		dictionary = Collections.emptyMap();
		bencodedData = "";
	}

	public AbstractMessage(int piece) {
		dictionary = Collections.emptyMap();
		Bencoder encode = new Bencoder();
		encode.dictionaryStart();
		encode.string("msg_type");
		encode.integer(getId());
		encode.string(PIECE_KEY);
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
		inStream.mark();
		String dictionaryString = inStream.readString(inStream.available());
		Bencode decoder = new Bencode(dictionaryString);

		dictionary = decoder.decodeDictionary();

		int readCharacters = dictionaryString.length() - decoder.remainingChars();
		inStream.resetToMark();
		inStream.skipBytes(readCharacters);
	}

}
