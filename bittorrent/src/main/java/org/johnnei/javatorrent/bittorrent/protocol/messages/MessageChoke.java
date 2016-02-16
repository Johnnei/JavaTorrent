package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

public class MessageChoke implements IMessage {

	@Override
	public void write(OutStream outStream) {
	}

	@Override
	public void read(InStream inStream) {
	}

	@Override
	public void process(Peer peer) {
		peer.setChoked(PeerDirection.Download, true);
		peer.cancelAllPieces();
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_CHOKE;
	}

	@Override
	public void setReadDuration(Duration duration) {
	}

	@Override
	public String toString() {
		return "Choke";
	}

}
