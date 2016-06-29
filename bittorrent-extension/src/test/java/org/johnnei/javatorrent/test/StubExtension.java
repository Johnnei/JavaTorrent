package org.johnnei.javatorrent.test;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class StubExtension implements IExtension {

	private final String name;

	public StubExtension(String name) {
		this.name = name;
	}

	@Override
	public IMessage getMessage(InStream data) {
		return new StubMessage();
	}

	@Override
	public void addHandshakeMetadata(Peer peer, BencodedMap bencoder) {
		/* Don't do anything in this stub */
	}

	@Override
	public void processHandshakeMetadata(Peer peer, BencodedMap dictionary, BencodedMap mEntry) {
		/* Don't do anything in this stub */
	}

	@Override
	public String getExtensionName() {
		return name;
	}

}