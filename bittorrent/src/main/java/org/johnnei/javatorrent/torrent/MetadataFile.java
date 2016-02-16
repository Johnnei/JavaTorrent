package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.johnnei.javatorrent.torrent.files.Piece;

public class MetadataFile extends AFiles {

	public static final int BLOCK_SIZE = 16384;

	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;

	public MetadataFile(Torrent torrent, File metadataFile) {
		this.fileInfos = new ArrayList<>();
		this.fileInfos.add(new FileInfo(fileSize, 0, metadataFile, 1));
		this.fileSize = (int) metadataFile.length();
		this.pieces = new ArrayList<>(1);
		this.pieces.add(new Piece(this, torrent.getHashArray(), 0, fileSize, BLOCK_SIZE));
	}

	@Override
	public boolean hasPiece(int pieceIndex) throws NoSuchElementException {
		return pieces.get(pieceIndex).isDone();
	}

	@Override
	public void havePiece(int pieceIndex) throws NoSuchElementException {
		Piece piece = pieces.get(pieceIndex);
		for (int i = 0; i < piece.getBlockCount(); i++) {
			piece.setDone(i);
		}
	}

	@Override
	public FileInfo getFileForBytes(int index, int blockIndex, int blockDataOffset) throws TorrentException {
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
	public byte[] getBitfieldBytes() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("UT_METADATA does not support bitfields.");
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
			RandomAccessFile fileAccess = fileInfos.get(0).getFileAcces();
			int blockSize = Math.min(BLOCK_SIZE, (int)(fileAccess.length() - blockOffset));
			byte[] data = new byte[blockSize];
			fileAccess.seek(blockOffset);
			fileAccess.readFully(data);
			return data;
		}
	}
}
