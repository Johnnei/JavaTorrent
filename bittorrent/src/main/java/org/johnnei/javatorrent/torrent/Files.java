package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Bitfield;
import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.utils.MathUtils;
import org.johnnei.javatorrent.utils.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Files extends AFiles {

	private static final Logger LOGGER = LoggerFactory.getLogger(Files.class);

	public static final int BLOCK_SIZE = 1 << 14;

	/**
	 * The folder name to put the files in
	 */
	private String folderName;
	/**
	 * The size of a standard block
	 */
	private int pieceSize;

	/**
	 * Centralised storage of have pieces
	 */
	private Bitfield bitfield;

	/**
	 * Creates a Files instance based upon a .torrent file
	 *
	 * @param torrentFile The metadata file containing the torrent information
	 * @param downloadFolder The folder in which the downloads need to be stored.
	 */
	public Files(File torrentFile, File downloadFolder) {
		parseTorrentFileData(torrentFile, downloadFolder);
		bitfield = new Bitfield(getBitfieldSize());
	}

	private void parseTorrentFileData(File torrentFile, File downloadFolder) {
		try (ByteInputStream in = new ByteInputStream(new FileInputStream(torrentFile))) {
			Bencode decoder = new Bencode(in.readString(in.available()));
			parseDictionary(decoder.decodeDictionary(), downloadFolder);
		} catch (IOException e) {
			LOGGER.warn("Failed to parse torrent data.", e);
			ThreadUtils.sleep(10);
			parseTorrentFileData(torrentFile, downloadFolder);
		}
	}

	private void parseDictionary(Map<String, Object> dictionary, File downloadFolder) throws IOException {
		new File(downloadFolder, dictionary.get("name") + System.lineSeparator()).mkdirs();

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
			int size = (remainingSize >= pieceSize) ? pieceSize : (int) remainingSize;
			byte[] sha1Hash = new byte[20];
			char[] hashBytes = pieceHashes.substring(hashOffset, hashOffset + 20).toCharArray();
			for (int i = 0; i < sha1Hash.length; i++) {
				sha1Hash[i] = (byte) hashBytes[i];
			}
			pieces.add(new Piece(this, sha1Hash, index, size, BLOCK_SIZE));
			remainingSize -= size;
		}
	}

	private long getNumberFromDictionary(Object o) {
		long l = 0L;
		if (o instanceof Integer) {
			l = ((int) o);
		} else {
			l = (long) o;
		}
		return l;
	}

	/**
	 * Adds the have to the bitfield and updates the correct fileInfo have counts
	 *
	 * @param pieceIndex
	 */
	@Override
	public void havePiece(int pieceIndex) {
		bitfield.havePiece(pieceIndex);
		if (!pieces.get(pieceIndex).isDone()) {
			for (int i = 0; i < pieces.get(pieceIndex).getBlockCount(); i++) {
				pieces.get(pieceIndex).setBlockStatus(i, BlockStatus.Verified);
			}
		}
	}

	public int getBitfieldSize() {
		return (int) Math.ceil(pieces.size() / 8D);
	}

	/**
	 * Gets the proper file location for the given filename
	 *
	 * @param name The desired file name
	 * @return The file within the download folder
	 */
	private File getFile(String name) {
		return new File(folderName + "/" + name);
	}

	@Override
	public Piece getPiece(int index) {
		return pieces.get(index);
	}

	public File getFolderName() {
		return new File(folderName);
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
	public int getBlockSize() {
		return BLOCK_SIZE;
	}

	/**
	 * Gets the FileInfo for the given piece and block
	 *
	 * @param pieceIndex The piece index
	 * @param blockIndex The block index within the piece
	 * @param byteOffset The offset within the block
	 * @return The FileInfo for the given data
	 * @throws IllegalArgumentException When information being requested is outside of this fileset.
	 */
	@Override
	public FileInfo getFileForBytes(int pieceIndex, int blockIndex, int byteOffset) {
		if (pieceIndex < 0 || blockIndex < 0 || byteOffset < 0) {
			throw new IllegalArgumentException("pieceIndex, blockIndex and byteOffset must all be >= 0.");
		}
		long bytesStartPosition = (pieceIndex * getPieceSize()) + (blockIndex * BLOCK_SIZE) + byteOffset;

		for (FileInfo fileInfo : fileInfos) {
			// If the file started before or at the wanted byteStartPosition then that file contains the bytes
			if (fileInfo.getFirstByteOffset() <= bytesStartPosition) {
				return fileInfo;
			}
		}

		throw new IllegalArgumentException("Piece is not within fileset.");
	}

	@Override
	public boolean hasPiece(int pieceIndex) throws NoSuchElementException {
		return bitfield.hasPiece(pieceIndex);
	}

	@Override
	public byte[] getBitfieldBytes() throws UnsupportedOperationException {
		return bitfield.getBytes();
	}

}
