package org.johnnei.javatorrent.test;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Assert;

public class StubMessage implements IMessage {

	@Override
	public void write(OutStream outStream) {
		outStream.writeByte(1);
	}

	@Override
	public void read(InStream inStream) {
		Assert.assertEquals("Incorrect payload", 1, inStream.readByte());
	}

	@Override
	public void process(Peer peer) {
		if (peer != null) {
			// Invoke this to allow for mocks to detect that this got called
			peer.getTorrent();
		}
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public void setReadDuration(Duration duration) {
		/* Nothing to do here for stub message */
	}

}
