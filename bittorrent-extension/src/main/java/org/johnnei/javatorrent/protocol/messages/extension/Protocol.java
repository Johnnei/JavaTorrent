package org.johnnei.javatorrent.protocol.messages.extension;

public class Protocol {

	private Protocol() {
		/* No instances for constants for you! */
	}

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
