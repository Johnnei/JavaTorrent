package org.johnnei.javatorrent.bittorrent.phases;

import java.util.Collection;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link PhaseRegulator} and {@link PhaseRegulator.Builder}
 */
@RunWith(EasyMockRunner.class)
public class PhaseRegulatorTest extends EasyMockSupport {

	@Test
	public void testCreateInitialPhase() {
		IDownloadPhase downloadPhaseMock = createMock(IDownloadPhase.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);

		replayAll();
		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> downloadPhaseMock, Optional.empty())
				.build();

		IDownloadPhase downloadPhase = regulator.createInitialPhase(torrentClientMock, torrentMock);

		verifyAll();

		assertEquals("Incorrect initial phase returned", downloadPhaseMock, downloadPhase);
	}

	@Test
	public void testCreateInitialPhaseOverridenInitial() {
		IDownloadPhase downloadPhaseMock = createMock(IDownloadPhase.class);
		SecondPhase secondPhaseMock = createMock(SecondPhase.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);

		replayAll();
		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> downloadPhaseMock, Optional.empty())
				.registerInitialPhase(SecondPhase.class, (client, torrent) -> secondPhaseMock, Optional.empty())
				.build();

		IDownloadPhase downloadPhase = regulator.createInitialPhase(torrentClientMock, torrentMock);

		verifyAll();

		assertEquals("Incorrect initial phase returned", secondPhaseMock, downloadPhase);
	}

	@Test
	public void testCreateNextPhase() {
		FirstPhase firstPhase = new FirstPhase();
		SecondPhase secondPhase = new SecondPhase();
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);

		replayAll();
		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(FirstPhase.class, (client, torrent) -> firstPhase, Optional.of(SecondPhase.class))
				.registerPhase(SecondPhase.class, (client, torrent) -> secondPhase, Optional.empty())
				.build();

		Optional<IDownloadPhase> downloadPhase = regulator.createNextPhase(firstPhase, torrentClientMock, torrentMock);
		Optional<IDownloadPhase> thirdPhase = regulator.createNextPhase(secondPhase, torrentClientMock, torrentMock);

		verifyAll();

		assertEquals("Incorrect initial phase returned", secondPhase, downloadPhase.get());
		assertFalse("Third phase got returned but wasn't configured.", thirdPhase.isPresent());
	}

	@Test
	public void testToString() {
		FirstPhase firstPhase = new FirstPhase();
		SecondPhase secondPhase = new SecondPhase();

		replayAll();
		PhaseRegulator regulator = new PhaseRegulator.Builder()
				.registerInitialPhase(IDownloadPhase.class, (client, torrent) -> firstPhase, Optional.of(SecondPhase.class))
				.registerPhase(SecondPhase.class, (client, torrent) -> secondPhase, Optional.empty())
				.build();

		verifyAll();

		assertTrue("Incorrect toString start", regulator.toString().startsWith("PhaseRegulator["));
	}

	@Test(expected = IllegalStateException.class)
	public void testDuplicatePhase() {
		FirstPhase firstPhase = new FirstPhase();

		new PhaseRegulator.Builder()
				.registerInitialPhase(FirstPhase.class, (client, torrent) -> firstPhase, Optional.empty())
				.registerPhase(FirstPhase.class, (client, torrent) -> firstPhase, Optional.empty());
	}

	@Test(expected = IllegalStateException.class)
	public void testBuildWithoutConfiguration() {
		new PhaseRegulator.Builder().build();
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
		public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
			return null;
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
		public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
			return null;
		}
	}

}