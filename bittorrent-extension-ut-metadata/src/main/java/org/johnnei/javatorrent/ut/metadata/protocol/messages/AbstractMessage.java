package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * Base class for ut_metadata messages.
 */
public abstract class AbstractMessage implements IMessage {

	private static final byte[] EMPTY_ARRAY = new byte[0];

	private Bencoding bencoding = new Bencoding();

	protected static final String PIECE_KEY = "piece";
	protected BencodedMap dictionary;
	protected byte[] bencodedData;

	/**
	 * Creates a new empty message.
	 */
	public AbstractMessage() {
		dictionary = new BencodedMap();
		bencodedData = EMPTY_ARRAY;
	}

	/**
	 * Creates a new message for the given piece.
	 * @param piece The piece for which this message will be send.
	 */
	public AbstractMessage(int piece) {
		dictionary = new BencodedMap();
		BencodedMap bencodedMap = new BencodedMap();
		bencodedMap.put("msg_type", new BencodedInteger(getId()));
		bencodedMap.put(PIECE_KEY, new BencodedInteger(piece));
		bencodedData = bencodedMap.serialize();
	}

	@Override
	public void write(OutStream outStream) {
		outStream.write(bencodedData);
	}

	@Override
	public void read(InStream inStream) {
		dictionary = (BencodedMap) bencoding.decode(inStream);
	}

}
