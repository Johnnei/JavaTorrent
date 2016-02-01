package org.johnnei.javatorrent.torrent.download;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagnetLink {

	private static final Logger LOGGER = LoggerFactory.getLogger(MagnetLink.class);

	/**
	 * The resulting torrent from this Magnet link
	 */
	private Torrent torrent;

	private TorrentBuilder torrentBuilder;

	private final TorrentClient torrentClient;

	public MagnetLink(String magnetLink, TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		this.torrentBuilder = new TorrentBuilder();

		if (!magnetLink.startsWith("magnet:?")) {
			return;
		}

		String[] linkData = magnetLink.split("\\?")[1].split("&");
		for (int i = 0; i < linkData.length; i++) {
			String[] data = linkData[i].split("=");
			switch (data[0]) {
			case "dn":
				linkData[i] = StringUtil.removeHex(StringUtil.spaceFix(data[1]));
				torrentBuilder.setDisplayName(linkData[1]);
				break;

			case "tr":
				linkData[i] = StringUtil.removeHex(data[1]);
				torrentBuilder.addTracker(linkData[i]);
				break;

			case "xt":
				String[] subdata = data[1].split(":");
				if (subdata.length < 3) {
					LOGGER.error("XT from MagnetLink is incomplete");
				} else if (!subdata[0].equals("urn")) {
					LOGGER.error("[XT] Expected a URN at position 0");
				} else if (!subdata[1].equals("btih")) {
					LOGGER.error("[XT] Unsupported Hashing: " + subdata[1]);
				} else if (subdata[2].length() != 40) {
					LOGGER.error("[XT] Invalid Hash length: " + subdata[2].length());
				} else {
					byte[] hash = new byte[20];
					for (int j = 0; j < subdata[2].length() / 2; j++) {
						hash[j] = (byte) Integer.parseInt(subdata[2].substring(j * 2, j * 2 + 2), 16);
					}
					torrentBuilder.setHash(hash);
				}
				break;

			default:
				LOGGER.warn("Unhandled Magnet Data: " + linkData[i]);
			}
		}
	}

	public Torrent getTorrent() throws IllegalStateException {
		if (torrent == null) {
			try {
				torrent = torrentBuilder.build(torrentClient);
			} catch (IllegalStateException e) {
				LOGGER.warn("Failed to build torrent from magnet link: " + e.getMessage());
			}
		}

		return torrent;
	}

	public boolean isDownloadable() {
		return torrentBuilder.isBuildable();
	}

}
