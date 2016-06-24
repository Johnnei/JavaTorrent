package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.util.Collections;
import java.util.Map;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
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
		BencodedMap bencodedMap = new BencodedMap();
		bencodedMap.put("msg_type", new BencodedInteger(getId()));
		bencodedMap.put(PIECE_KEY, new BencodedInteger(piece));
		bencodedData = bencodedMap.serialize();
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
