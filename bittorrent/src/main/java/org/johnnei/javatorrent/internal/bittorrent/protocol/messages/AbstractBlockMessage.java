package org.johnnei.javatorrent.internal.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * The base class for messages which contains a request about a block.
 */
public abstract class AbstractBlockMessage implements IMessage {

	protected int index;
	protected int offset;
	protected int length;

	@Override
	public void write(OutStream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeInt(length);
	}

	@Override
	public void read(InStream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		length = inStream.readInt();
	}

	@Override
	public int getLength() {
		return 13;
	}
}
