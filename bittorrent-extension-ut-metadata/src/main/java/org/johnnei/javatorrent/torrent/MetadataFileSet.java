package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.util.ArrayList;

import org.johnnei.javatorrent.internal.torrent.MetadataFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.Argument;

public class MetadataFileSet extends AbstractFileSet {

	public static final int BLOCK_SIZE = 16384;

	private MetadataFileSetRequestFactory requestFactory;

	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;

	public MetadataFileSet(Torrent torrent, File metadataFile) {
		super(BLOCK_SIZE);
		Argument.requireNonNull(torrent, "Torrent cannot be null.");
		Argument.requireNonNull(metadataFile, "Metadata file cannot be null.");
		if (!metadataFile.exists()) {
			throw new IllegalArgumentException("Metadata file must exist.");
		}

		requestFactory = new MetadataFileSetRequestFactory();

		this.fileSize = (int) metadataFile.length();
		this.fileInfos = new ArrayList<>();
		this.fileInfos.add(new FileInfo(fileSize, 0, metadataFile, 1));
		this.pieces = new ArrayList<>(1);
		this.pieces.add(new Piece(this, torrent.getMetadata().getHash(), 0, fileSize, BLOCK_SIZE));
	}

	@Override
	public long getPieceSize() {
		return fileSize;
	}

	@Override
	public byte[] getBitfieldBytes() {
		throw new UnsupportedOperationException("UT_METADATA does not support bitfield.");
	}

	@Override
	public IFileSetRequestFactory getRequestFactory() {
		return requestFactory;
	}
}
