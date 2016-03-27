package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.internal.torrent.peer.Bitfield;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.MathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentFileSet extends AbstractFileSet {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentFileSet.class);

	private static final int BLOCK_SIZE = 1 << 14;

	private static final String ERR_INCOMPLETE_INFO_ENTRY
			= "Metadata file appears to be validly encoded but is missing critical information from the 'info' entry.";

	/**
	 * The folder name to put the files in
	 */
	private File downloadFolder;
	/**
	 * The size of a standard block
	 */
	private int pieceSize;

	/**
	 * Centralised storage of have pieces
	 */
	private Bitfield bitfield;

	/**
	 * Creates a TorrentFileSet instance based upon a .torrent file
	 *
	 * @param torrentFile The metadata file containing the torrent information
	 * @param downloadFolder The folder in which the downloads need to be stored.
	 * @throws IllegalArgumentException When the torrent file is missing or incomplete.
	 */
	public TorrentFileSet(File torrentFile, File downloadFolder) {
		super(BLOCK_SIZE);
		Argument.requireNonNull(torrentFile, "Torrent file can not be null");

		if (!torrentFile.exists()) {
			throw new IllegalArgumentException(String.format("Torrent file (%s) does not exist.", torrentFile.getAbsolutePath()));
		}

		this.downloadFolder = Argument.requireNonNull(downloadFolder, "Download folder cannot be null");

		parseTorrentFileData(torrentFile);
		bitfield = new Bitfield(getBitfieldSize());
	}

	private void parseTorrentFileData(File torrentFile) {
		try (ByteInputStream in = new ByteInputStream(new FileInputStream(torrentFile))) {
			Bencode decoder = new Bencode(in.readString(in.available()));

			Map<String, Object> metadataInfo = decoder.decodeDictionary();
			if (!isInfoDirectory(metadataInfo)) {
				if (!metadataInfo.containsKey("info")) {
					throw new IllegalArgumentException(ERR_INCOMPLETE_INFO_ENTRY);
				}

				metadataInfo = (Map<String, Object>) metadataInfo.get("info");
				if (!isInfoDirectory(metadataInfo)) {
					throw new IllegalArgumentException(ERR_INCOMPLETE_INFO_ENTRY);
				}
			}

			parseDictionary(metadataInfo);
		} catch (IOException e) {
			LOGGER.warn("Failed to parse torrent data.", e);
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				LOGGER.trace("Got interrupted while parsing torrent information.", ex);
			}
			parseTorrentFileData(torrentFile);
		}
	}

	private void parseDictionary(Map<String, Object> dictionary) throws IOException {
		if (dictionary.containsKey("name")) {
			downloadFolder = new File(downloadFolder, (String) dictionary.get("name"));
		}

		if (!downloadFolder.mkdirs()) {
			throw new IOException(String.format("Failed to create download folder: %s", downloadFolder.getAbsolutePath()));
		}

		pieceSize = (int) dictionary.get("piece length");
		long remainingSize = 0L;

		if (dictionary.containsKey("files")) { // Multi-file torrent
			ArrayList<?> files = (ArrayList<?>) dictionary.get("files");
			fileInfos = new ArrayList<>(files.size());
			for (int i = 0; i < files.size(); i++) {
				HashMap<?, ?> file = (HashMap<?, ?>) files.get(i);
				long fileSize = getNumberFromDictionary(file.get("length"));
				ArrayList<?> fileStructure = (ArrayList<?>) file.get("path");
				String fileName = "";
				if (fileStructure.size() > 1) {
					for (int j = 0; j < fileStructure.size(); j++) {
						fileName += "/" + fileStructure.get(j);
					}
				} else {
					fileName = (String) fileStructure.get(0);
				}
				int pieceCount = (int) MathUtils.ceilDivision(fileSize, pieceSize);
				if (remainingSize % pieceSize != 0 && fileSize >= pieceSize) {
					pieceCount++;
				}
				FileInfo info = new FileInfo(fileSize, remainingSize, getFile(fileName), pieceCount);
				fileInfos.add(info);
				remainingSize += fileSize;
			}
		} else { // Single file torrent
			fileInfos = new ArrayList<>(1);
			String filename = (String) dictionary.get("name");
			long fileSize = getNumberFromDictionary(dictionary.get("length"));
			fileInfos.add(new FileInfo(fileSize, remainingSize, getFile(filename), (int) MathUtils.ceilDivision(fileSize, pieceSize)));
			remainingSize += fileSize;
		}
		String pieceHashes = (String) dictionary.get("pieces");
		int pieceAmount = pieceHashes.length() / 20;
		pieces = new ArrayList<>(pieceAmount);
		for (int index = 0; index < pieceAmount; index++) {
			int hashOffset = index * 20;
			int size = (int) Math.min(pieceSize, remainingSize);
			byte[] sha1Hash = new byte[20];
			char[] hashBytes = pieceHashes.substring(hashOffset, hashOffset + 20).toCharArray();
			for (int i = 0; i < sha1Hash.length; i++) {
				sha1Hash[i] = (byte) hashBytes[i];
			}
			pieces.add(new Piece(this, sha1Hash, index, size, BLOCK_SIZE));
			remainingSize -= size;
		}
	}

	private boolean isInfoDirectory(Map<String, Object> metadata) {
		if (!metadata.containsKey("length") && !metadata.containsKey("files")) {
			return false;
		}

		if (!metadata.containsKey("pieces")) {
			return false;
		}

		if (!metadata.containsKey("piece length")) {
			return false;
		}

		return true;
	}

	private long getNumberFromDictionary(Object o) {
		if (o instanceof Integer) {
			return ((int) o);
		} else {
			return (long) o;
		}
	}

	/**
	 * Adds the have to the bitfield and updates the correct fileInfo have counts
	 *
	 * @param pieceIndex
	 */
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
		return pieceSize;
	}

	@Override
	public byte[] getBitfieldBytes() throws UnsupportedOperationException {
		return bitfield.getBytes();
	}

}
