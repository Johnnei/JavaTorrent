package org.johnnei.javatorrent.magnetlink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagnetLink {

	private static final Logger LOGGER = LoggerFactory.getLogger(MagnetLink.class);

	private static final Pattern BTIH_BASE16_PATTERN = Pattern.compile("urn:btih:([A-Z0-9]{40})");
	private static final Pattern BTIH_BASE32_PATTERN = Pattern.compile("urn:btih:([A-Z0-9]{32})");

	private final TorrentClient torrentClient;

	/**
	 * The resulting torrent from this Magnet link
	 */
	private Torrent torrent;

	private Torrent.Builder torrentBuilder;

	private Collection<String> trackerUrls;

	public MagnetLink(String magnetLink, TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		this.torrentBuilder = new Torrent.Builder();
		this.torrentBuilder.setTorrentClient(torrentClient);
		trackerUrls = new ArrayList<>();

		if (!magnetLink.startsWith("magnet:?")) {
			return;
		}

		String[] linkSections = magnetLink.split("\\?", 2)[1].split("&");
		for (int i = 0; i < linkSections.length; i++) {
			String[] data = linkSections[i].split("=", 2);
			final String key = data[0];
			final String value = data[1];

			switch (key) {
			case "dn":
				torrentBuilder.setName(StringUtil.removeHex(StringUtil.spaceFix(value)));
				break;

			case "tr":
				trackerUrls.add(StringUtil.removeHex(value));
				break;

			case "xt":
				extractHash(value);
				break;

			default:
				LOGGER.warn("Unhandled Magnet Data: " + linkSections[i]);
			}
		}
	}

	private void extractHash(String value) {
		Matcher matcher = BTIH_BASE16_PATTERN.matcher(value);
		if (matcher.find()) {
			torrentBuilder.setHash(extractBase16Hash(matcher.group(1)));
			return;
		}

		matcher = BTIH_BASE32_PATTERN.matcher(value);
		if (matcher.find()) {
			torrentBuilder.setHash(convertBase32Hash(matcher.group(1)));
			return;
		}

		LOGGER.error("Failed to parse XT entry of magnet link.");
	}

	private byte[] extractBase16Hash(String hashSection) {
		byte[] hash = new byte[20];
		for (int j = 0; j < hashSection.length() / 2; j++) {
			hash[j] = (byte) Integer.parseInt(hashSection.substring(j * 2, j * 2 + 2), 16);
		}
		return hash;
	}

	private byte[] convertBase32Hash(String hashSection) {
		throw new IllegalStateException("Base 32 hashes are not yet supported.");
	}

	public Torrent getTorrent() throws IllegalStateException {
		if (torrent == null) {
			torrent = torrentBuilder.build();
			trackerUrls.stream().forEach(url -> torrentClient.getTrackerManager().addTorrent(torrent, url));
		}

		return torrent;
	}

	public boolean isDownloadable() {
		return torrentBuilder.canDownload();
	}

}
