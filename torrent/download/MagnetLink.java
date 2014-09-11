package torrent.download;

import torrent.Manager;
import torrent.download.tracker.Tracker;
import torrent.util.StringUtil;

public class MagnetLink {

	/**
	 * Returns if this Magnet link is valid "enough" to be downloaded with JavaTorrent
	 */
	private boolean downloadable;
	/**
	 * The resulting torrent from this Magnet link
	 */
	private Torrent torrent;

	public MagnetLink(String magnetLink, Manager manager) {
		boolean succeed = true;
		if (magnetLink.startsWith("magnet:?")) {
			String[] linkData = magnetLink.split("\\?")[1].split("&");
			torrent = new Torrent(manager);
			for (int i = 0; i < linkData.length; i++) {
				String[] data = linkData[i].split("=");
				switch (data[0]) {
				case "dn":
					linkData[i] = StringUtil.removeHex(StringUtil.spaceFix(data[1]));
					torrent.setDisplayName(linkData[1]);
					break;

				case "tr":
					linkData[i] = StringUtil.removeHex(data[1]);
					Tracker tracker = manager.getTrackerManager().addTorrent(torrent, linkData[i]);
					torrent.addTracker(tracker);
					break;

				case "xt":
					String[] subdata = data[1].split(":");
					if (subdata.length < 3) {
						succeed = false;
						System.err.println("XT from MagnetLink is incomplete");
					} else if (!subdata[0].equals("urn")) {
						succeed = false;
						System.err.println("[XT] Expected a URN at position 0");
					} else if (!subdata[1].equals("btih")) {
						succeed = false;
						System.err.println("[XT] Unsupported Hashing: " + subdata[1]);
					} else if (subdata[2].length() != 40) {
						succeed = false;
						System.err.println("[XT] Invalid Hash length: " + subdata[2].length());
					} else {
						byte[] hash = new byte[20];
						for (int j = 0; j < subdata[2].length() / 2; j++) {
							hash[j] = (byte) Integer.parseInt(subdata[2].substring(j * 2, j * 2 + 2), 16);
						}
						torrent.setHash(hash);
					}
					break;

				default:
					System.err.println("Unhandled Magnet Data: " + linkData[i]);
				}
				downloadable = succeed;
			}
			if(!torrent.hasHash()) {
				System.err.println("Magnet link has no hash");
				downloadable = false;
			} else if(!torrent.hasTracker()) {
				System.err.println("Manget link has no tracker");
				downloadable = false;
			}
		} else {
			downloadable = false;
		}
	}

	public Torrent getTorrent() {
		return torrent;
	}

	public boolean isDownloadable() {
		return downloadable;
	}

}
