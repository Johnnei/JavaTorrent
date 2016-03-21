package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.util.ArrayList;

import org.johnnei.javatorrent.torrent.files.Piece;

public class MetadataFileSet extends AbstractFileSet {

	public static final int BLOCK_SIZE = 16384;

	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;

	public MetadataFileSet(Torrent torrent, File metadataFile) {
		super(BLOCK_SIZE);
		this.fileInfos = new ArrayList<>();
		this.fileInfos.add(new FileInfo(fileSize, 0, metadataFile, 1));
		this.fileSize = (int) metadataFile.length();
		this.pieces = new ArrayList<>(1);
		this.pieces.add(new Piece(this, torrent.getHashArray(), 0, fileSize, BLOCK_SIZE));
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
	public byte[] getBitfieldBytes() {
		throw new UnsupportedOperationException("UT_METADATA does not support bitfield.");
	}
}
