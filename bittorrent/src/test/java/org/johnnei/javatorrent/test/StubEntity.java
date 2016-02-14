package org.johnnei.javatorrent.test;

import java.io.File;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.AFiles;
import org.johnnei.javatorrent.torrent.download.FileInfo;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.algos.IPeerManager;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.tracker.IPeerConnector;

import org.easymock.EasyMockSupport;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;

public class StubEntity {

	public static TorrentClient stubTorrentClient(EasyMockSupport context) {
		IPeerConnector peerConnectorMock = context.createMock(IPeerConnector.class);
		IPeerManager peerManagerMock = context.createMock(IPeerManager.class);
		TorrentClient torrentClientMock = context.createMock(TorrentClient.class);
		PhaseRegulator phaseRegulatorMock = context.createMock(PhaseRegulator.class);
		IDownloadPhase downloadPhaseMock = context.createMock(IDownloadPhase.class);
		ExecutorService service = new ExecutorServiceMock();

		// Setup getters
		expect(torrentClientMock.getPeerConnector()).andStubReturn(peerConnectorMock);
		expect(torrentClientMock.getExecutorService()).andStubReturn(service);
		expect(torrentClientMock.getPeerManager()).andStubReturn(peerManagerMock);
		expect(torrentClientMock.getPhaseRegulator()).andStubReturn(phaseRegulatorMock);
		expect(phaseRegulatorMock.createInitialPhase(eq(torrentClientMock), notNull())).andStubReturn(downloadPhaseMock);

		return torrentClientMock;
	}

	/**
	 * Creates a file stub which reports having no files
	 * @param pieceCount the amount of pieces reported
	 * @return
	 */
	public static AFiles stubAFiles(int pieceCount) {
		return new AFilesStub(pieceCount);
	}

	public static IPeerManager stubPeerManager() {
		return new PeerManagerStub();
	}

	private static final class AFilesStub extends AFiles {

		public AFilesStub(int pieceCount) {
			fileInfos = new ArrayList<>();
			fileInfos.add(new FileInfo(1, 0, new File("./target/tmp/afilesstub.tmp"), pieceCount));
			pieces = new ArrayList<>();
			for (int i = 0; i < pieceCount; i++) {
				pieces.add(new Piece(this, new byte[] {}, i, 1, 1));
			}
		}

		@Override
		public void havePiece(int pieceIndex) throws NoSuchElementException {
		}

		@Override
		public boolean hasPiece(int pieceIndex) throws NoSuchElementException {
			return false;
		}

		@Override
		public long getPieceSize() {
			return 0;
		}

		@Override
		public FileInfo getFileForBytes(int index, int blockIndex, int blockDataOffset) throws TorrentException {
			return null;
		}

		@Override
		public int getBlockSize() {
			return 0;
		}

		@Override
		public byte[] getBitfieldBytes() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

	}

	private static class PeerManagerStub implements IPeerManager {

		@Override
		public int getMaxPeers() {
			return 5;
		}

		@Override
		public int getMaxPendingPeers() {
			return 2;
		}

		@Override
		public int getAnnounceWantAmount(int connected) {
			return 3;
		}

		@Override
		public String getName() {
			return "PMStub";
		}

	}

}
