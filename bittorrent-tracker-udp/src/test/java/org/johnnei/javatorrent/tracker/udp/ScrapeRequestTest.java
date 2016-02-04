package org.johnnei.javatorrent.tracker.udp;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.johnnei.javatorrent.test.TestUtils.copySection;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.util.Collections;
import java.util.Optional;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class ScrapeRequestTest extends EasyMockSupport {

	@Test
	public void testWriteRequest() {
		Torrent torrentOne = DummyEntity.createTorrent();
		Torrent torrentTwo = DummyEntity.createTorrent();
		Torrent torrentThree = DummyEntity.createTorrent();

		byte[] expectedOutput = new byte[60];
		copySection(torrentOne.getHashArray(), expectedOutput, 0);
		copySection(torrentTwo.getHashArray(), expectedOutput, 20);
		copySection(torrentThree.getHashArray(), expectedOutput, 40);

		ScrapeRequest request = new ScrapeRequest(asList(torrentOne, torrentTwo, torrentThree));
		OutStream outStream = new OutStream();

		request.writeRequest(outStream);

		byte[] actual = outStream.toByteArray();

		assertEquals("Unexpected request length", expectedOutput.length, actual.length);
		assertArrayEquals("Incorrect output", expectedOutput, actual);
	}

	@Test
	public void testReadAndProcessRequest() throws Exception {
		Torrent torrentOne = DummyEntity.createTorrent();
		Torrent torrentTwo = DummyEntity.createTorrent();
		Torrent torrentThree = DummyEntity.createTorrent();

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
		UdpTracker trackerMock = createMock(UdpTracker.class);

		expect(trackerMock.getInfo(eq(torrentOne))).andReturn(Optional.of(infoOne));
		expect(trackerMock.getInfo(eq(torrentTwo))).andReturn(Optional.empty());
		expect(trackerMock.getInfo(eq(torrentThree))).andReturn(Optional.of(infoTwo));

		replayAll();

		request.readResponse(inStream);
		request.process(trackerMock);

		verifyAll();

		assertEquals("Incorrect seeders", 1, infoOne.getSeeders());
		assertEquals("Incorrect download count", "2", infoOne.getDownloadCount());
		assertEquals("Incorrect leechers", 3, infoOne.getLeechers());

		assertEquals("Incorrect seeders", 7, infoTwo.getSeeders());
		assertEquals("Incorrect download count", "8", infoTwo.getDownloadCount());
		assertEquals("Incorrect leechers", 9, infoTwo.getLeechers());
	}

	@Test(expected=TrackerException.class)
	public void testReadAndProcessBrokenRequest() throws Exception {
		Torrent torrentOne = DummyEntity.createTorrent();
		Torrent torrentTwo = DummyEntity.createTorrent();
		Torrent torrentThree = DummyEntity.createTorrent();

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
		UdpTracker trackerMock = createMock(UdpTracker.class);
		replayAll();

		request.readResponse(inStream);
		request.process(trackerMock);

		verifyAll();
	}

	@Test
	public void testMinimalSize() {
		ScrapeRequest request = new ScrapeRequest(Collections.emptyList());
		assertEquals("Incorrect minimal size.", 0, request.getMinimalSize());
	}

}
