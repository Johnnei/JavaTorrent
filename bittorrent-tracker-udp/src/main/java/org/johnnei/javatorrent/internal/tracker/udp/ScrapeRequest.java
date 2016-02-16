package org.johnnei.javatorrent.internal.tracker.udp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrapeRequest implements IUdpTrackerPayload {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapeRequest.class);

	private List<Torrent> torrents;

	private List<ScrapeResult> results;

	public ScrapeRequest(List<Torrent> torrents) {
		this.torrents = Objects.requireNonNull(torrents);
		this.results = new ArrayList<>(torrents.size());
	}

	@Override
	public void writeRequest(OutStream outStream) {
		torrents.stream()
				.map(torrent -> torrent.getHashArray())
				.forEach(outStream::writeByte);
	}

	@Override
	public void readResponse(InStream inStream) throws TrackerException {
		final int bytesPerTorrent = 12;
		if (inStream.available() / bytesPerTorrent != torrents.size()) {
			throw new TrackerException(String.format(
					"Incorrect amount of bytes returned. Expected: %d, Got: %d",
					bytesPerTorrent * torrents.size(),
					inStream.available()));
		}

		while (inStream.available() >= 12) {
			int seeders = inStream.readInt();
			int completed = inStream.readInt();
			int leechers = inStream.readInt();
			results.add(new ScrapeResult(seeders, completed, leechers));
		}
	}

	@Override
	public void process(UdpTracker tracker) {
		for (int index = 0; index < torrents.size(); index++) {
			Torrent torrent = torrents.get(index);
			ScrapeResult result = results.get(index);
			Optional<TorrentInfo> info = tracker.getInfo(torrent);

			if (!info.isPresent()) {
				LOGGER.warn(String.format("Requested scrape for %s which is no longer registered for the tracker.", torrent));
				continue;
			}

			info.get().setInfo(result.seeders, result.leechers, result.completed);
		}
	}

	@Override
	public TrackerAction getAction() {
		return TrackerAction.SCRAPE;
	}

	@Override
	public int getMinimalSize() {
		return 0;
	}

	private static final class ScrapeResult {

		private final int seeders;

		private final int completed;

		private final int leechers;

		public ScrapeResult(final int seeders, final int completed, final int leechers) {
			this.seeders = seeders;
			this.completed = completed;
			this.leechers = leechers;
		}
	}

}
