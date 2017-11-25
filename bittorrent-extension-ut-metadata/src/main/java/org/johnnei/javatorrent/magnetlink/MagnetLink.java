package org.johnnei.javatorrent.magnetlink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagnetLink {

	private static final Base32 base32 = new Base32(32);

	private static final Logger LOGGER = LoggerFactory.getLogger(MagnetLink.class);

	private static final PatternToExtractor[] SUPPORTED_BASES = {
			new PatternToExtractor(Pattern.compile("urn:btih:([a-fA-F0-9]{40})"), MagnetLink::convertBase16Hash),
			new PatternToExtractor(Pattern.compile("urn:btih:([a-zA-Z0-9]{32})"), base32::decode)
	};

	/**
	 * The resulting torrent from this Magnet link
	 */
	private Torrent torrent;

	private boolean hasName;

	private Torrent.Builder torrentBuilder;

	private Collection<String> trackerUrls;

	public MagnetLink(String magnetLink, TorrentClient torrentClient) {
		this.torrentBuilder = new Torrent.Builder();
		this.torrentBuilder.setTorrentClient(torrentClient);
		trackerUrls = new ArrayList<>();

		if (!magnetLink.startsWith("magnet:?")) {
			throw new IllegalArgumentException("Format does not comply with a magnet link.");
		}

		String[] linkSections = magnetLink.split("\\?", 2)[1].split("&");

		for (int i = 0; i < linkSections.length; i++) {
			String[] data = linkSections[i].split("=", 2);
			if (data.length != 2) {
				throw new IllegalArgumentException(String.format("Section does not comply with a magnet link: %s", linkSections[i]));
			}

			final String key = data[0];
			final String value = data[1];

			switch (key) {
				case "dn":
					torrentBuilder.setName(decodeEncodedCharacters(spaceFix(value)));
					hasName = true;
					break;

				case "tr":
					trackerUrls.add(decodeEncodedCharacters(value));
					break;

				case "xt":
					extractHash(value);
					break;

				default:
					LOGGER.warn("Unhandled magnet link section: {}", linkSections[i]);
			}
		}
	}

	private void extractHash(String value) {
		for (PatternToExtractor base : SUPPORTED_BASES) {
			Matcher matcher = base.getBasePattern().matcher(value);

			if (matcher.find()) {
				String hashString = matcher.group(1);

				if (!hasName) {
					torrentBuilder.setName(hashString);
				}

				torrentBuilder.setMetadata(new Metadata.Builder().setHash(base.getExtractFunction().apply(hashString.toUpperCase())).build());
				return;
			}
		}

		throw new IllegalArgumentException("Failed to parse XT entry of magnet link.");
	}

	private static byte[] convertBase16Hash(String hashSection) {
		byte[] hash = new byte[20];
		for (int j = 0; j < hashSection.length() / 2; j++) {
			hash[j] = (byte) Integer.parseInt(hashSection.substring(j * 2, j * 2 + 2), 16);
		}
		return hash;
	}

	/**
	 * Translates the hexadecimal encoding back to readable text
	 *
	 * @param s
	 * @return
	 */
	private String decodeEncodedCharacters(String s) {
		String[] pieces = s.split("%");
		for (int i = 1; i < pieces.length; i++) {
			int hex = Integer.parseInt(pieces[i].substring(0, 2), 16);
			pieces[i] = (char) hex + pieces[i].substring(2);
		}

		StringBuilder result = new StringBuilder();
		for (String piece : pieces) {
			result.append(piece);
		}
		return result.toString();
	}

	/**
	 * Replaces all occurrences of "+" with " "
	 *
	 * @param s
	 * @return
	 */
	private static String spaceFix(String s) {
		return s.replaceAll("\\+", " ");
	}

	/**
	 * Gets the torrent which this magnet link produced.
	 * @return The torrent based on this magnet link.
	 *
	 * @throws IllegalStateException When {@link #isDownloadable()} returns false.
	 */
	public Torrent getTorrent() {
		if (!isDownloadable()) {
			throw new IllegalStateException("Torrent information is incomplete.");
		}
		if (torrent == null) {
			torrent = torrentBuilder.build();
		}

		return torrent;
	}

	/**
	 * Gets the collection of trackers which are present in the parsed magnet link.
	 * @return The collection of trackers.
	 */
	public Collection<String> getTrackerUrls() {
		return Collections.unmodifiableCollection(trackerUrls);
	}

	/**
	 * Tests if this magnetlink has enough information to be downloadable.
	 * @return <code>true</code> when the hash is available.
	 *
	 * @see Torrent.Builder#canDownload()
	 */
	public boolean isDownloadable() {
		return torrentBuilder.canDownload();
	}

	private static final class PatternToExtractor {

		private final Pattern basePattern;

		private final Function<String, byte[]> extractFunction;

		PatternToExtractor(Pattern basePattern, Function<String, byte[]> extractFunction) {
			this.basePattern = basePattern;
			this.extractFunction = extractFunction;
		}

		Pattern getBasePattern() {
			return basePattern;
		}

		Function<String, byte[]> getExtractFunction() {
			return extractFunction;
		}
	}
}
