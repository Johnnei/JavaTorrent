package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

/**
 * The class capable of translating the {@link UtpProtocol} constants to {@link IPayload} instances.
 */
public class UtpPayloadFactory {

	/**
	 * Creates a new instance for the given <code>type</code>
	 * @param type The type as defined in {@link UtpProtocol}
	 * @return A newly created instance of the {@link IPayload} associated with the given <code>type</code>
	 */
	public IPayload createPayloadFromType(int type) {
		switch (type) {
			case UtpProtocol.ST_DATA:
				return new DataPayload();

			case UtpProtocol.ST_FIN:
				return new FinPayload();

			case UtpProtocol.ST_RESET:
				return new ResetPayload();

			case UtpProtocol.ST_STATE:
				return new StatePayload();

			case UtpProtocol.ST_SYN:
				return new SynPayload();

			default:
				throw new IllegalArgumentException("Invalid Packet id " + type + " for uTP Protocol");
		}
	}

}
