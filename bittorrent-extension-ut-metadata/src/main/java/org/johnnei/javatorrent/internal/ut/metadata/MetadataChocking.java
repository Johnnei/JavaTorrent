package org.johnnei.javatorrent.internal.ut.metadata;

import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public final class MetadataChocking implements IChokingStrategy {

	@Override
	public void updateChoking(Peer peer) {
		boolean expectedInterestedState = peer.getModuleInfo(MetadataInformation.class).map(MetadataInformation::getMetadataSize).orElse(0L) > 0L;

		if (peer.isInterested(PeerDirection.Download) != expectedInterestedState) {
			peer.setInterested(PeerDirection.Download, expectedInterestedState);
		}

		if (!peer.isChoked(PeerDirection.Upload)) {
			peer.setChoked(PeerDirection.Upload, true);
		}
	}

}
