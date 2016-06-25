package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.io.StringReader;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessage implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessage.class);

	private Bencoding bencoding = new Bencoding();

	protected static final String PIECE_KEY = "piece";
	protected BencodedMap dictionary;
	protected String bencodedData;

	public AbstractMessage() {
		dictionary = new BencodedMap();
		bencodedData = "";
	}

	public AbstractMessage(int piece) {
		dictionary = new BencodedMap();
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
		StringReader reader = new StringReader(inStream.readString(inStream.available()));
		inStream.resetToMark();

		dictionary = (BencodedMap) bencoding.decode(reader);
		inStream.skipBytes(bencoding.getCharactersRead());
	}

}
