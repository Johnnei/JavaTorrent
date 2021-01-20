package org.johnnei.javatorrent.phases;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.NopPrioritizer;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link PhaseRegulator} and {@link PhaseRegulator.Builder}
 */
public class PhaseRegulatorTest {

	@Test
	public void testCreateInitialPhase() {
		IDownloadPhase downloadPhaseMock = mock(IDownloadPhase.class);
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> downloadPhaseMock)
				.build();

		IDownloadPhase downloadPhase = regulator.createInitialPhase(torrentClientMock, torrentMock);

		assertEquals(downloadPhaseMock, downloadPhase, "Incorrect initial phase returned");
	}

	@Test
	public void testCreateInitialPhaseOverridenInitial() {
		IDownloadPhase downloadPhaseMock = mock(IDownloadPhase.class);
		SecondPhase secondPhaseMock = mock(SecondPhase.class);
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> downloadPhaseMock)
				.registerInitialPhase(SecondPhase.class, (client, torrent) -> secondPhaseMock)
				.build();

		IDownloadPhase downloadPhase = regulator.createInitialPhase(torrentClientMock, torrentMock);

		assertEquals(secondPhaseMock, downloadPhase, "Incorrect initial phase returned");
	}

	@Test
	public void testCreateNextPhase() {
		FirstPhase firstPhase = new FirstPhase();
		SecondPhase secondPhase = new SecondPhase();
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(FirstPhase.class, (client, torrent) -> firstPhase, SecondPhase.class)
				.registerPhase(SecondPhase.class, (client, torrent) -> secondPhase)
				.build();

		Optional<IDownloadPhase> downloadPhase = regulator.createNextPhase(firstPhase, torrentClientMock, torrentMock);
		Optional<IDownloadPhase> thirdPhase = regulator.createNextPhase(secondPhase, torrentClientMock, torrentMock);

		assertEquals(secondPhase, downloadPhase.get(), "Incorrect initial phase returned");
		assertFalse(thirdPhase.isPresent(), "Third phase got returned but wasn't configured.");
	}

	@Test
	public void testToString() {
		FirstPhase firstPhase = new FirstPhase();
		SecondPhase secondPhase = new SecondPhase();

		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> firstPhase, SecondPhase.class)
				.registerPhase(SecondPhase.class, (client, torrent) -> secondPhase)
				.build();

		assertTrue(regulator.toString().startsWith("PhaseRegulator["), "Incorrect toString start");
	}

	@Test
	public void testDuplicatePhase() {
		FirstPhase firstPhase = new FirstPhase();

		assertThrows(IllegalStateException.class, () -> new PhaseRegulator.Builder()
				.registerInitialPhase(FirstPhase.class, (client, torrent) -> firstPhase)
				.registerPhase(FirstPhase.class, (client, torrent) -> firstPhase));
	}

	@Test
	public void testBuildWithoutConfiguration() {
		assertThrows(IllegalStateException.class, () -> new PhaseRegulator.Builder().build());
	}

	private static class FirstPhase implements IDownloadPhase {

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public void process() {
		}

		@Override
		public void onPhaseEnter() {
		}

		@Override
		public void onPhaseExit() {
		}

		@Override
		public IChokingStrategy getChokingStrategy() {
			return null;
		}

		@Override
		public boolean isPeerSupportedForDownload(Peer peer) {
			return true;
		}

		@Override
		public Optional<AbstractFileSet> getFileSet() {
			return Optional.empty();
		}

		@Override
		public PiecePrioritizer getPiecePrioritizer() {
			return new NopPrioritizer();
		}
	}

	private static class SecondPhase implements IDownloadPhase {

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public void process() {
		}

		@Override
		public void onPhaseEnter() {
		}

		@Override
		public void onPhaseExit() {
		}

		@Override
		public IChokingStrategy getChokingStrategy() {
			return null;
		}

		@Override
		public boolean isPeerSupportedForDownload(Peer peer) {
			return true;
		}

		@Override
		public Optional<AbstractFileSet> getFileSet() {
			return Optional.empty();
		}

		@Override
		public PiecePrioritizer getPiecePrioritizer() {
			return new NopPrioritizer();
		}
	}

}
