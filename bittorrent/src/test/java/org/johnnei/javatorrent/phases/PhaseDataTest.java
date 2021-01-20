package org.johnnei.javatorrent.phases;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.internal.torrent.TorrentFileSetRequestFactory;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(TempFolderExtension.class)
public class PhaseDataTest {

	private ITracker createTrackerExpectingSetCompleted(Torrent torrent) {
		ITracker trackerMock = mock(ITracker.class);
		TorrentInfo torrentInfoMock = mock(TorrentInfo.class);

		when(trackerMock.getInfo(same(torrent))).thenReturn(Optional.of(torrentInfoMock));
		torrentInfoMock.setEvent(eq(TrackerEvent.EVENT_COMPLETED));
		return trackerMock;
	}

	@Test
	public void testOnPhaseExit() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		ITracker trackerOne = createTrackerExpectingSetCompleted(torrentMock);
		ITracker trackerTwo = createTrackerExpectingSetCompleted(torrentMock);

		when(torrentClientMock.getTrackersFor(same(torrentMock))).thenReturn(Arrays.asList(trackerOne, trackerTwo));

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseExit();
	}

	@Test
	public void testOnPhaseEnter(@Folder Path temporaryFolder) throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(temporaryFolder.toFile());

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).checkProgress();
	}

	@Test
	public void testOnPhaseEnterCreateFolder(@Folder Path temporaryFolder) throws IOException {
		File file = temporaryFolder.resolve("myFolder").toFile();

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(file);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).checkProgress();
		assertTrue(file.exists(), "Download folder should have been created.");
	}

	@Test
	public void testOnPhaseEnterCreateFolderFailure() throws IOException {
		File fileMock = mock(File.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(fileMock);

		when(fileMock.exists()).thenReturn(false);
		when(fileMock.mkdirs()).thenReturn(false);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		Exception e = assertThrows(TorrentException.class, cut::onPhaseEnter);
		assertThat(e.getMessage(), containsString("download folder"));

		verify(torrentMock).checkProgress();
	}

	@Test
	public void testIsDone() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.isDone()).thenReturn(true);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);

		assertTrue(cut.isDone(), "File set should have returned done, so the phase should have been done");
	}
}
