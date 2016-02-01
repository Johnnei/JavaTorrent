package org.johnnei.javatorrent.test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;

import java.io.File;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.AFiles;
import org.johnnei.javatorrent.torrent.download.FileInfo;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.algos.IPeerManager;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.tracker.IPeerConnector;

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

	private static final class AFilesStub extends AFiles {

		public AFilesStub(int pieceCount) {
			fileInfos = new ArrayList<>();
			fileInfos.add(new FileInfo(1, 0, new File("./mock.tmp"), pieceCount));
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

}
