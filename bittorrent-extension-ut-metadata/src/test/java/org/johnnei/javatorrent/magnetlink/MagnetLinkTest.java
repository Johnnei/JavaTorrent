package org.johnnei.javatorrent.magnetlink;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;

import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link MagnetLink}
 */
public class MagnetLinkTest extends EasyMockSupport {

	private Logger LOGGER = LoggerFactory.getLogger(MagnetLinkTest.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMinimalBase32Link() {
		String[] links = new String[] {
				"magnet:?xt=urn:btih:za3j6c5ex5wnq75rhm2do6bofr4cbozy",
				"magnet:?xt=urn:btih:ZA3J6C5EX5WNQ75RHM2DO6BOFR4CBOZY",
				"magnet:?xt=urn:btih:za3J6C5EX5wNq75RhM2DO6Bofr4CBoZY"
		};

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		for (String link : links) {
			LOGGER.info("Test case: {}", link);
			byte[] expectedHash = new byte[]{
					(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
					0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

			MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);


			assertTrue("Torrent is complete enough to download", magnetLink.isDownloadable());
			assertEquals("No tracker listed, list should be empty", 0, magnetLink.getTrackerUrls().size());

			Torrent torrent = magnetLink.getTorrent();
			assertArrayEquals("Incorrect hash", expectedHash, torrent.getMetadata().getHash());
		}

		verifyAll();
	}

	@Test
	public void testMinimalBase16Link() {
		String[] links = new String[] {
				"magnet:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38",
				"magnet:?xt=urn:btih:C8369F0BA4BF6CD87FB13B3437782E2C7820BB38",
				"magnet:?xt=urn:btih:c8369f0BA4bf6cd87FB13b3437782E2C7820bb38"
		};

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		for (String link : links) {
			LOGGER.info("Test case: {}", link);
			byte[] expectedHash = new byte[]{
					(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
					0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

			MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);


			assertTrue("Torrent is complete enough to download", magnetLink.isDownloadable());
			assertEquals("No tracker listed, list should be empty", 0, magnetLink.getTrackerUrls().size());

			Torrent torrent = magnetLink.getTorrent();
			assertArrayEquals("Incorrect hash", expectedHash, torrent.getMetadata().getHash());
		}

		verifyAll();
	}

	@Test
	public void testBase16LinkWithNameAndTrackerAndUnknownSection() {
		String link = "magnet:?dn=GIMP+2.8.16-setup-1.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38&nope=yes&tr=udp://localhost:80";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		verifyAll();

		assertTrue("Torrent is complete enough to download", magnetLink.isDownloadable());
		assertTrue("Tracker udp://localhost:80 is missing", magnetLink.getTrackerUrls().contains("udp://localhost:80"));

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals("Incorrect hash", expectedHash, torrent.getMetadata().getHash());
		assertEquals("Incorrect name", "GIMP 2.8.16-setup-1.exe", torrent.getDisplayName());
	}

	@Test
	public void testBase16LinkWithNameAndTracker() {
		String link = "magnet:?dn=GIMP+2.8.16-setup-1.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38&tr=udp://localhost:80";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		verifyAll();

		assertTrue("Torrent is complete enough to download", magnetLink.isDownloadable());
		assertTrue("Tracker udp://localhost:80 is missing", magnetLink.getTrackerUrls().contains("udp://localhost:80"));

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals("Incorrect hash", expectedHash, torrent.getMetadata().getHash());
		assertEquals("Incorrect name", "GIMP 2.8.16-setup-1.exe", torrent.getDisplayName());
	}

	@Test
	public void testLinkWithTwoTrackers() {
		String link = "magnet:?tr=udp://localhost:80&tr=udp://localhost:8080";

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		verifyAll();

		assertTrue("Tracker udp://localhost:80 is missing", magnetLink.getTrackerUrls().contains("udp://localhost:80"));
		assertTrue("Tracker udp://localhost:8080 is missing", magnetLink.getTrackerUrls().contains("udp://localhost:8080"));
	}

	@Test
	public void testLinkWithEncodedCharacters() {
		String link = "magnet:?dn=GIMP%202.8.16%2Bsetup-1%23GA.exe&xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";
		byte[] expectedHash = new byte[]{
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8, 0x7f, (byte) 0xb1,
				0x3b, 0x34, 0x37, 0x78, 0x2e, 0x2c, 0x78, 0x20, (byte) 0xbb, 0x38};

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		verifyAll();

		assertTrue("Torrent is complete enough to download", magnetLink.isDownloadable());
		assertEquals("No tracker listed, list should be empty", 0, magnetLink.getTrackerUrls().size());

		Torrent torrent = magnetLink.getTorrent();
		assertArrayEquals("Incorrect hash", expectedHash, torrent.getMetadata().getHash());
		assertEquals("Incorrect name", "GIMP 2.8.16+setup-1#GA.exe", torrent.getDisplayName());
	}

	@Test
	public void testCacheTorrentResult() {
		String link = "magnet:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);
		assertTrue("Torrent instance should be cached", magnetLink.getTorrent() == magnetLink.getTorrent());
	}

	@Test
	public void testIncorrectMagnetLink() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Format does not comply with");

		String link = "torrent:?xt=urn:btih:c8369f0ba4bf6cd87fb13b3437782e2c7820bb38";

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		new MagnetLink(link, torrentClientMock);
	}

	@Test
	public void testIncompleteSection() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Section does not comply with");

		String link = "magnet:?xt";

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		new MagnetLink(link, torrentClientMock);
	}

	@Test
	public void testBuildUndownloadableTorrent() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Torrent information is incomplete.");

		String link = "magnet:?dn=fail";

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		MagnetLink magnetLink = new MagnetLink(link, torrentClientMock);

		assertFalse("Torrent does not have hash", magnetLink.isDownloadable());
		magnetLink.getTorrent();
	}
}