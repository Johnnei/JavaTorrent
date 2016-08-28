package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.johnnei.javatorrent.internal.torrent.TorrentFileSetRequestFactory;
import org.johnnei.javatorrent.internal.torrent.peer.Bitfield;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.MathUtils;

public class TorrentFileSet extends AbstractFileSet {

	private static final int BLOCK_SIZE = 1 << 14;

	private final TorrentFileSetRequestFactory requestFactory;

	private final Metadata metadata;

	/**
	 * The folder name to put the files in
	 */
	private File downloadFolder;

	/**
	 * Centralised storage of have pieces
	 */
	private Bitfield bitfield;

	/**
	 * Creates a TorrentFileSet instance based upon a .torrent file
	 *
	 * @param metadata The metadata containing the torrent information
	 * @param downloadFolder The folder in which the downloads need to be stored.
	 * @throws IllegalArgumentException When the torrent file is missing or incomplete.
	 */
	public TorrentFileSet(Metadata metadata, File downloadFolder) {
		super(BLOCK_SIZE);
		this.metadata = Argument.requireNonNull(metadata, "Torrent metadata can not be null");
		this.downloadFolder = Argument.requireNonNull(downloadFolder, "Download folder cannot be null");

		requestFactory = new TorrentFileSetRequestFactory();

		long remainingSize = 0L;
		fileInfos = new ArrayList<>(metadata.getFileEntries().size());
		for (FileEntry fileEntry : metadata.getFileEntries()) {
			int pieceCount = (int) MathUtils.ceilDivision(fileEntry.getSize(), metadata.getPieceSize());
			FileInfo info = new FileInfo(fileEntry.getSize(), fileEntry.getFirstByteOffset(), getFile(fileEntry.getFileName()), pieceCount);
			fileInfos.add(info);

			remainingSize += fileEntry.getSize();
		}

		pieces = new ArrayList<>(metadata.getPieceHashes().size());
		List<byte[]> pieceHashes = metadata.getPieceHashes();

		for (int index = 0; index < pieceHashes.size(); index++) {
			int pieceSize = (int) Math.min(remainingSize, metadata.getPieceSize());
			pieces.add(new Piece(this, pieceHashes.get(index), index, pieceSize, BLOCK_SIZE));
			remainingSize -= pieceSize;
		}

		assert remainingSize == 0 : "Filesize and piece counts doesn't match up";

		bitfield = new Bitfield(getBitfieldSize());
	}

	@Override
	public void setHavingPiece(int pieceIndex) {
		super.setHavingPiece(pieceIndex);
		bitfield.havePiece(pieceIndex);
	}

	private int getBitfieldSize() {
		return (int) Math.ceil(pieces.size() / 8D);
	}

	/**
	 * Gets the proper file location for the given filename
	 *
	 * @param name The desired file name
	 * @return The file within the download folder
	 */
	private File getFile(String name) {
		return new File(downloadFolder, name);
	}

	/**
	 * Gets the default piece size
	 *
	 * @return The default piece size
	 */
	@Override
	public long getPieceSize() {
		return metadata.getPieceSize();
	}

	@Override
	public byte[] getBitfieldBytes() {
		return bitfield.getBytes();
	}

	@Override
	public TorrentFileSetRequestFactory getRequestFactory() {
		return requestFactory;
	}

	public File getDownloadFolder() {
		return downloadFolder;
	}
}
