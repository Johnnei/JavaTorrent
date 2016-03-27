package org.johnnei.javatorrent.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.tracker.IPeerConnector;

import org.easymock.EasyMockSupport;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;

public class StubEntity {

	public static TorrentClient stubTorrentClient(EasyMockSupport context) {
		IPeerConnector peerConnectorMock = context.createMock(IPeerConnector.class);
		TorrentClient torrentClientMock = context.createMock(TorrentClient.class);
		PhaseRegulator phaseRegulatorMock = context.createMock(PhaseRegulator.class);
		IDownloadPhase downloadPhaseMock = context.createMock(IDownloadPhase.class);
		ScheduledExecutorService service = new ExecutorServiceMock();

		// Setup getters
		expect(torrentClientMock.getPeerConnector()).andStubReturn(peerConnectorMock);
		expect(torrentClientMock.getExecutorService()).andStubReturn(service);
		expect(torrentClientMock.getPhaseRegulator()).andStubReturn(phaseRegulatorMock);
		expect(phaseRegulatorMock.createInitialPhase(eq(torrentClientMock), notNull())).andStubReturn(downloadPhaseMock);

		return torrentClientMock;
	}

	/**
	 * Creates a file stub which reports having no files
	 * @param pieceCount the amount of pieces reported
	 * @return
	 */
	public static AbstractFileSet stubAFiles(int pieceCount) {
		return new AFilesStub(pieceCount);
	}

	public static AbstractFileSet stubAFiles(int pieceCount, FileInfo fileInfo) {
		return new AFilesStub(pieceCount, fileInfo);
	}

	private static final class AFilesStub extends AbstractFileSet {

		public AFilesStub(int pieceCount, FileInfo defaultFile) {
			this(pieceCount);
			fileInfos = Collections.singletonList(defaultFile);
		}

		public AFilesStub(int pieceCount) {
			super(1);
			fileInfos = new ArrayList<>();
			fileInfos.add(new FileInfo(1, 0, new File("./target/tmp/afilesstub.tmp"), pieceCount));
			pieces = new ArrayList<>();
			for (int i = 0; i < pieceCount; i++) {
				pieces.add(new Piece(this, new byte[] {}, i, 1, 1));
			}
		}

		@Override
		public void setHavingPiece(int pieceIndex) throws NoSuchElementException {
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
		public FileInfo getFileForBytes(int index, int blockIndex, int blockDataOffset) {
			return fileInfos.get(0);
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
