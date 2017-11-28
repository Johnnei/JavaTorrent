package org.johnnei.javatorrent.internal.tracker.udp;

import java.time.Clock;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.UdpTracker;

import static java.util.Arrays.asList;
import static org.johnnei.javatorrent.test.TestUtils.copySection;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScrapeRequestTest {

	@Test
	public void testWriteRequest() {
		Torrent torrentOne = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrentOne);
		Torrent torrentThree = DummyEntity.createUniqueTorrent(torrentOne, torrentTwo);

		byte[] expectedOutput = new byte[60];
		copySection(torrentOne.getMetadata().getHash(), expectedOutput, 0);
		copySection(torrentTwo.getMetadata().getHash(), expectedOutput, 20);
		copySection(torrentThree.getMetadata().getHash(), expectedOutput, 40);

		ScrapeRequest request = new ScrapeRequest(asList(torrentOne, torrentTwo, torrentThree));
		OutStream outStream = new OutStream();

		request.writeRequest(outStream);

		byte[] actual = outStream.toByteArray();

		assertEquals(expectedOutput.length, actual.length, "Unexpected request length");
		assertArrayEquals(expectedOutput, actual, "Incorrect output");
	}

	@Test
	public void testReadAndProcessRequest() throws Exception {
		Torrent torrentOne = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent();
		Torrent torrentThree = DummyEntity.createUniqueTorrent();

		TorrentInfo infoOne = new TorrentInfo(torrentOne, Clock.systemDefaultZone());
		TorrentInfo infoTwo = new TorrentInfo(torrentThree, Clock.systemDefaultZone());

		byte[] inputBytes = {
				// Torrent one
				0x00, 0x00, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x02,
				0x00, 0x00, 0x00, 0x03,
				// Torrent two
				0x00, 0x00, 0x00, 0x04,
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x00, 0x06,
				// Torrent three
				0x00, 0x00, 0x00, 0x07,
				0x00, 0x00, 0x00, 0x08,
				0x00, 0x00, 0x00, 0x09,
		};

		ScrapeRequest request = new ScrapeRequest(asList(torrentOne, torrentTwo, torrentThree));
		InStream inStream = new InStream(inputBytes);
		UdpTracker trackerMock = mock(UdpTracker.class);

		when(trackerMock.getInfo(eq(torrentOne))).thenReturn(Optional.of(infoOne));
		when(trackerMock.getInfo(eq(torrentTwo))).thenReturn(Optional.empty());
		when(trackerMock.getInfo(eq(torrentThree))).thenReturn(Optional.of(infoTwo));

		request.readResponse(inStream);
		request.process(trackerMock);

		assertEquals(1, infoOne.getSeeders(), "Incorrect seeders");
		assertEquals("2", infoOne.getDownloadCount(), "Incorrect download count");
		assertEquals(3, infoOne.getLeechers(), "Incorrect leechers");

		assertEquals(7, infoTwo.getSeeders(), "Incorrect seeders");
		assertEquals("8", infoTwo.getDownloadCount(), "Incorrect download count");
		assertEquals(9, infoTwo.getLeechers(), "Incorrect leechers");
	}

	@Test
	public void testReadAndProcessBrokenRequest() throws Exception {
		Torrent torrentOne = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent();
		Torrent torrentThree = DummyEntity.createUniqueTorrent();

		byte[] inputBytes = {
				// Torrent one
				0x00, 0x00, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x02,
				0x00, 0x00, 0x00, 0x03,
				// Torrent two
				0x00, 0x00, 0x00, 0x04,
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x00, 0x06,
				// Torrent three
				0x00, 0x00, 0x00, 0x07,
				0x00, 0x00, 0x00, 0x08,
				0x00, 0x00, 0x00,
		};

		ScrapeRequest request = new ScrapeRequest(asList(torrentOne, torrentTwo, torrentThree));
		InStream inStream = new InStream(inputBytes);

		assertThrows(TrackerException.class, () -> request.readResponse(inStream));
	}

	@Test
	public void testMinimalSize() {
		ScrapeRequest request = new ScrapeRequest(Collections.emptyList());
		assertEquals(0, request.getMinimalSize(), "Incorrect minimal size.");
	}

}
