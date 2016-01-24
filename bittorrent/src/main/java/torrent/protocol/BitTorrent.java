package torrent.protocol;

public final class BitTorrent {

	/**
	 * The extension bytes which indicates which extension we support
	 */
	public static final byte[] RESERVED_EXTENTION_BYTES = new byte[8];

	static {
		// TODO Rework this into a modular system
		// Initialise reserved bytes field
		RESERVED_EXTENTION_BYTES[5] |= 0x10; // Extended Messages
	}

	/*
	 * There is a message without ID. This message only contains a length integer. The message is used as keep-alive
	 */

	/**
	 * Used to notify the peer that we choked them<br/>
	 * No payload
	 */
	public static final int MESSAGE_CHOKE = 0;
	/**
	 * Used to notify the peer that we unchoked them<br/>
	 * No payload
	 */
	public static final int MESSAGE_UNCHOKE = 1;
	/**
	 * Used to notify the peer that we got interest in them<br/>
	 * No payload
	 */
	public static final int MESSAGE_INTERESTED = 2;
	/**
	 * Used to notify the peer that we got no interest in them<br/>
	 * No payload
	 */
	public static final int MESSAGE_UNINTERESTED = 3;
	/**
	 * Notify the peer that we have a piece<br/>
	 * Payload:<br/>
	 * 1x: (u)int - Piece Index<br/>
	 */
	public static final int MESSAGE_HAVE = 4;
	/**
	 * Used as a quick-have message.<br/>
	 * Only allowed just after an handshake<br/>
	 * Each bit represents the have state of a piece: 1 - have, 0 - not have.<br/>
	 * Payload:<br/>
	 * x times: ubyte - Bitfield data<br/>
	 * (x will be Math.ceil(pieceCount / 8);)<br/>
	 * The last byte will be padded with zero's so the message will be exactly x bytes
	 */
	public static final int MESSAGE_BITFIELD = 5;
	/**
	 * Request a piece part from the peer<br/>
	 * Payload:<br/>
	 * uint index - Piece index<br/>
	 * uint offset - Offset within the piece<br/>
	 * uint length - The amount of data (Commenly 2^14/2^15), I use 2^14
	 */
	public static final int MESSAGE_REQUEST = 6;
	/**
	 * The data of a piece<br/>
	 * Payload:<br/>
	 * uint index - Piece Index<br/>
	 * uint offset - Offset within the piece<br/>
	 * x ubyte - The data, the length is equal the rest of the message bytes
	 */
	public static final int MESSAGE_PIECE = 7;
	/**
	 * Cancel a send piece request<br/>
	 * payload:<br/>
	 * uint index<br/>
	 * uint offset<br/>
	 * uint length<br/>
	 * <br/>
	 */
	public static final int MESSAGE_CANCEL = 8;
	/**
	 * This has something to do with DHT<br/>
	 * payload:<br/>
	 * listen-port<br/>
	 * <br/>
	 * <b>This is not yet implemented in this client</b>
	 */
	public static final int MESSAGE_PORT = 9;
	/**
	 * An Protocol Extension message<br/>
	 * Payload:<br/>
	 * ubyte Extended message id<br/>
	 * string bencoded dictionary<br/>
	 * <br/>
	 * <i>See the other files in this package to see which extensions are supported</i><br/>
	 * <br/>
	 * <i>The extendeded messages all send a dictionary with the extra data.<br/>
	 * Expect every extended message to have it, any payloads noted will be extra</i>
	 */
	public static final int MESSAGE_EXTENDED_MESSAGE = 20;
	/**
	 * Extended Message handshake<br/>
	 * Used to share the Extension packet ID's with eachother<br/>
	 * payload:<br/>
	 * string bencoded dictionary<br/>
	 */
	public static final int EXTENDED_MESSAGE_HANDSHAKE = 0;

}
