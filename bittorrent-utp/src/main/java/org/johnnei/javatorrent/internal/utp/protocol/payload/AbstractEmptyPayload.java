package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * Most types defined in {@link org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol} don't contain any payload bytes.
 * This base class defines {@link #write(OutStream)} and {@link #read(InStream)} to do nothing.
 */
public abstract class AbstractEmptyPayload implements IPayload {

	@Override
	public void read(InStream inStream) {
		// This payload has no extra data.
	}

	@Override
	public void write(OutStream outStream) {
		// This payload has no extra data.
	}

	@Override
	public int getSize() {
		return 0;
	}
}
