package torrent.download.algos;

import torrent.download.Torrent;

public class BurstPeerManager implements IPeerManager {

	private int maxPeers;
	private float burstRate;

	public BurstPeerManager(int maxPeers, float burstRate) {
		this.maxPeers = maxPeers;
		this.burstRate = burstRate;
	}

	@Override
	public int getMaxPeers(byte torrentState) {
		if (torrentState == Torrent.STATE_DOWNLOAD_METADATA)
			return (int) (maxPeers * 0.5D);
		return maxPeers;
	}

	@Override
	public int getAnnounceWantAmount(byte torrentState, int connected) {
		return (int) ((maxPeers - connected) * burstRate);
	}

	@Override
	public String getName() {
		return "Burst Peer Manager (Max: " + maxPeers + ", Burst: " + (int) (maxPeers * burstRate) + ")";
	}

	@Override
	public int getMaxPendingPeers(byte torrentState) {
		if (torrentState == Torrent.STATE_DOWNLOAD_METADATA)
			return (int) (maxPeers * 0.5D);
		return (int) (maxPeers * burstRate);
	}

}
