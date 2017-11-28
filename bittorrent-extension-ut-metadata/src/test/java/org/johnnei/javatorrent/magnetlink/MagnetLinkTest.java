package org.johnnei.javatorrent.magnetlink;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test {@link MagnetLink}
 */
public class MagnetLinkTest {

	@ParameterizedTest
	@MethodSource("hashCases")
	public void testMinimalBase32Link(String link) {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		byte[] expectedHash = new byte[]{
			(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
			0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38 };

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertTrue(magnetLink.isDownloadable(), "Torrent is complete enough to download");
		assertEquals(0, magnetLink.getTrackerUrls().size(), "No tracker listed, list should be empty");

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals(expectedHash, torrent.getMetadata().getHash(), "Incorrect hash");
	}

	public static Stream<String> hashCases() {
		return Stream.of(
			// Base 32
			"magnet:?xt=urn:btih:za3j6c5ex5wnq75rhm2do6bofr4cbozy",
			"magnet:?xt=urn:btih:ZA3J6C5EX5WNQ75RHM2DO6BOFR4CBOZY",
			"magnet:?xt=urn:btih:za3J6C5EX5wNq75RhM2DO6Bofr4CBoZY",
			// Base 16
			"magnet:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38",
			"magnet:?xt=urn:btih:C8369F0BA4BF6CD87FB13B3437782E2C7820BB38",
			"magnet:?xt=urn:btih:c8369f0BA4bf6cd87FB13b3437782E2C7820bb38"
		);
	}

	@Test
	public void testBase16LinkWithNameAndTrackerAndUnknownSection() {
		String link = "magnet:?dn=GIMP+2.8.16-setup-1.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38&nope=yes&tr=udp://localhost:80";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertTrue(magnetLink.isDownloadable(), "Torrent is complete enough to download");
		assertTrue(magnetLink.getTrackerUrls().contains("udp://localhost:80"), "Tracker udp://localhost:80 is missing");

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals(expectedHash, torrent.getMetadata().getHash(), "Incorrect hash");
		assertEquals("GIMP 2.8.16-setup-1.exe", torrent.getDisplayName(), "Incorrect name");
	}

	@Test
	public void testBase16LinkWithNameAndTracker() {
		String link = "magnet:?dn=GIMP+2.8.16-setup-1.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38&tr=udp://localhost:80";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertTrue(magnetLink.isDownloadable(), "Torrent is complete enough to download");
		assertTrue(magnetLink.getTrackerUrls().contains("udp://localhost:80"), "Tracker udp://localhost:80 is missing");

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals(expectedHash, torrent.getMetadata().getHash(), "Incorrect hash");
		assertEquals("GIMP 2.8.16-setup-1.exe", torrent.getDisplayName(), "Incorrect name");
	}

	@Test
	public void testLinkWithTwoTrackers() {
		String link = "magnet:?tr=udp://localhost:80&tr=udp://localhost:8080";

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertTrue(magnetLink.getTrackerUrls().contains("udp://localhost:80"), "Tracker udp://localhost:80 is missing");
		assertTrue(magnetLink.getTrackerUrls().contains("udp://localhost:8080"), "Tracker udp://localhost:8080 is missing");
	}

	@Test
	public void testLinkWithEncodedCharacters() {
		String link = "magnet:?dn=GIMP%202.8.16%2Bsetup-1%23GA.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertTrue(magnetLink.isDownloadable(), "Torrent is complete enough to download");
		assertEquals(0, magnetLink.getTrackerUrls().size(), "No tracker listed, list should be empty");

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals(expectedHash, torrent.getMetadata().getHash(), "Incorrect hash");
		assertEquals("GIMP 2.8.16+setup-1#GA.exe", torrent.getDisplayName(), "Incorrect name");
	}

	@Test
	public void testCacheTorrentResult() {
		String link = "magnet:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);
		assertThat("Torrent instance should be cached", magnetLink.getTorrent(), is(magnetLink.getTorrent()));
	}

	@Test
	public void testIncorrectMagnetLink() {
		String link = "torrent:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		Exception e = assertThrows(IllegalArgumentException.class, () -> new MagnetLink(link, torrentClientMock));
		assertThat(e.getMessage(), containsString("Format does not comply with"));
	}

	@Test
	public void testIncompleteSection() {
		String link = "magnet:?xt";

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		assertThrows(IllegalArgumentException.class, () -> new MagnetLink(link, torrentClientMock));
	}

	@Test
	public void testBuildUndownloadableTorrent() {
		String link = "magnet:?dn=fail";

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertFalse(magnetLink.isDownloadable(), "Torrent does not have hash");
		Exception e = assertThrows(IllegalStateException.class, magnetLink::getTorrent);
		assertThat(e.getMessage(), containsString("Torrent information is incomplete."));
	}
}
