package org.johnnei.javatorrent.internal.utp.protocol.packet;

public abstract class DatalessPayload implements Payload {

	private static final byte[] EMPTY_DATA = new byte[0];

	@Override
	public byte[] getData() {
		return EMPTY_DATA;
	}
}
