package org.johnnei.javatorrent.torrent.download.algos;

public class BurstPeerManager implements IPeerManager {

	private int maxPeers;
	private float burstRate;

	public BurstPeerManager(int maxPeers, float burstRate) {
		this.maxPeers = maxPeers;
		this.burstRate = burstRate;
	}

	@Override
	public int getMaxPeers() {
		return maxPeers;
	}

	@Override
	public int getAnnounceWantAmount(int connected) {
		return (int) ((maxPeers - connected) * burstRate);
	}

	@Override
	public String getName() {
		return "Burst Peer Manager (Max: " + maxPeers + ", Burst: " + (int) (maxPeers * burstRate) + ")";
	}

	@Override
	public int getMaxPendingPeers() {
		return (int) Math.ceil(maxPeers * burstRate);
	}

}
