package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;

public class MetadataFileSet extends AbstractFileSet {

	public static final int BLOCK_SIZE = 16384;

	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;

	public MetadataFileSet(Torrent torrent, File metadataFile) {
		this.fileInfos = new ArrayList<>();
		this.fileInfos.add(new FileInfo(fileSize, 0, metadataFile, 1));
		this.fileSize = (int) metadataFile.length();
		this.pieces = new ArrayList<>(1);
		this.pieces.add(new Piece(this, torrent.getHashArray(), 0, fileSize, BLOCK_SIZE));
	}

	@Override
	public boolean hasPiece(int pieceIndex) {
		return pieces.get(pieceIndex).isDone();
	}

	@Override
	public void havePiece(int pieceIndex) {
		Piece piece = pieces.get(pieceIndex);
		for (int i = 0; i < piece.getBlockCount(); i++) {
			piece.setBlockStatus(i, BlockStatus.Verified);
		}
	}

	@Override
	public FileInfo getFileForBytes(int index, int blockIndex, int blockDataOffset) {
		return fileInfos.get(0);
	}

	@Override
	public long getPieceSize() {
		return fileSize;
	}

	@Override
	public int getBlockSize() {
		return BLOCK_SIZE;
	}

	@Override
	public byte[] getBitfieldBytes() {
		throw new UnsupportedOperationException("UT_METADATA does not support bitfield.");
	}

	 /**
	 * Gets a block from the metadata file
	 *
	 * @param piece The block
	 * @return The 16384 (or less if it is the last block) bytes needed to answer the request
	 * @throws IOException When reading the file fails for whatever reason
	 */
	public byte[] getBlock(int piece) throws IOException {
		int blockOffset = piece * BLOCK_SIZE;
		synchronized (fileInfos.get(0).FILE_LOCK) {
			RandomAccessFile fileAccess = fileInfos.get(0).getFileAccess();
			int blockSize = Math.min(BLOCK_SIZE, (int)(fileAccess.length() - blockOffset));
			byte[] data = new byte[blockSize];
			fileAccess.seek(blockOffset);
			fileAccess.readFully(data);
			return data;
		}
	}
}
