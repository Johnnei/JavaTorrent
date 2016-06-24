package org.johnnei.javatorrent.internal.bittorrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

public class AbstractBlockMessage {

	protected int index;
	protected int offset;
	protected int length;

	public void write(OutStream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeInt(length);
	}

	public void read(InStream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		length = inStream.readInt();
	}

	public int getLength() {
		return 13;
	}
}
