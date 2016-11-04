package org.johnnei.javatorrent.torrent.algos.requests;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Interface which influences the {@link Peer#setRequestLimit(int)}
 */
public interface IRequestLimiter {

	void onReceivedBlock(Peer peer, MessageBlock messageBlock);
}
