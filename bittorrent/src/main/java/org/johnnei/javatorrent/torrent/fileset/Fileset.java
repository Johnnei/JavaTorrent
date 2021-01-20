package org.johnnei.javatorrent.torrent.fileset;

import org.johnnei.javatorrent.torrent.peer.Peer;

public interface Fileset {

	boolean hasPiece(Peer peer, int pieceIndex);

}
