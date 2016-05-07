package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;

public class UtpPayloadFactory {

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
