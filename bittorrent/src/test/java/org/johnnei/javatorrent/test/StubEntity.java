package org.johnnei.javatorrent.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.FileInfo;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

@Deprecated
public class StubEntity {

	@Deprecated
	public static AbstractFileSet stubAFiles(int pieceCount, FileInfo fileInfo) {
		return new AFilesStub(pieceCount, fileInfo);
	}

	@Deprecated
	public static AbstractFileSet stubAFiles(int pieceCount, FileInfo fileInfo, int blockSize) {
		return new AFilesStub(pieceCount, fileInfo, blockSize);
	}

	@Deprecated
	private static final class AFilesStub extends AbstractFileSet {

		private int blockSize;

		public AFilesStub(int pieceCount, FileInfo defaultFile, int blockSize) {
			this(pieceCount, defaultFile);
			this.blockSize = blockSize;
		}
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
		public IFileSetRequestFactory getRequestFactory() {
			return null;
		}

		@Override
		public void setHavingPiece(int pieceIndex) throws NoSuchElementException {
		}

		@Override
		public boolean hasPiece(int pieceIndex) throws NoSuchElementException {
			return false;
		}

		@Override
		public boolean hasPiece(Peer peer, int pieceIndex) {
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
			return blockSize;
		}

		@Override
		public byte[] getBitfieldBytes() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

	}
}
