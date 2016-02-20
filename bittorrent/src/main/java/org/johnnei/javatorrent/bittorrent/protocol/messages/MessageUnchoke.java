package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public class MessageUnchoke implements IMessage {

	@Override
	public void write(OutStream outStream) {
	}

	@Override
	public void read(InStream inStream) {
	}

	@Override
	public void process(Peer peer) {
		peer.setChoked(PeerDirection.Download, false);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_UNCHOKE;
	}

	@Override
	public String toString() {
		return "Unchoke";
	}

}
