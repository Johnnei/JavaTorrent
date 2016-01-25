package torrent.download;

import java.util.Collection;
import java.util.LinkedList;

import org.johnnei.javatorrent.TorrentClient;

public class TorrentBuilder {

	private byte[] btihHash;
	private String displayName;

	/**
	 * The collection of tracker urls
	 */
	private Collection<String> trackers;

	public TorrentBuilder() {
		trackers = new LinkedList<>();
	}

	public void setHash(byte[] btihHash) {
		this.btihHash = btihHash;
	}

	public void addTracker(String trackerUrl) {
		trackers.add(trackerUrl);
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Creates the {@link Torrent} derived from the given parameters in previous calls
	 * @param torrentClient
	 * @return
	 * @throws IllegalStateException if the torrent has to few information to start downloading
	 */
	public Torrent build(TorrentClient torrentClient) throws IllegalStateException {
		if (btihHash == null) {
			throw new IllegalStateException("Missing SHA-1 hash for torrent");
		}

		if (trackers.isEmpty()) {
			throw new IllegalStateException("Missing trackers for torrent");
		}

		Torrent torrent = new Torrent(torrentClient, btihHash, displayName);

		trackers.forEach(trackerUrl -> {
			torrentClient.getTrackerManager().addTorrent(torrent, trackerUrl);
		});

		return torrent;
	}

	public boolean isBuildable() {
		if (btihHash == null) {
			return false;
		}

		if (trackers.isEmpty()) {
			return false;
		}

		return true;
	}

}
