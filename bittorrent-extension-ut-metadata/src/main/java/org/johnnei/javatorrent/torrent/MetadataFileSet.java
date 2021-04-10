package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.johnnei.javatorrent.internal.torrent.MetadataFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;

public class MetadataFileSet extends AbstractFileSet {

	public static final int BLOCK_SIZE = 16384;

	private final MetadataFileSetRequestFactory requestFactory;

	/**
	 * The size of the metadata file in total
	 */
	private final int fileSize;

	public MetadataFileSet(byte[] btihHash, Path metadataFile) {
		super(BLOCK_SIZE);
		Argument.requireNonNull(btihHash, "Hash cannot be null.");
		Argument.requireNonNull(metadataFile, "Metadata file cannot be null.");
		if (Files.notExists(metadataFile)) {
			throw new IllegalArgumentException("Metadata file must exist.");
		}

		requestFactory = new MetadataFileSetRequestFactory();

		try {
			this.fileSize = (int) Files.size(metadataFile);
		} catch (IOException e) {
			throw new TorrentException("Failed to read metadata size: " + metadataFile.toAbsolutePath(), e);
		}
		this.fileInfos = new ArrayList<>();
		this.fileInfos.add(new FileInfo(fileSize, 0, metadataFile.toFile(), 1));
		this.pieces = new ArrayList<>(1);
		this.pieces.add(new Piece(this, btihHash, 0, fileSize, BLOCK_SIZE));
	}

	@Override
	public boolean hasPiece(Peer peer, int pieceIndex) {
		return true;
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
