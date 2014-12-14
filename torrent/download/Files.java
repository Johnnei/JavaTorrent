package torrent.download;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.johnnei.utils.JMath;
import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.TorrentException;
import torrent.download.files.Piece;
import torrent.download.peer.Bitfield;
import torrent.encoding.Bencode;

public class Files extends AFiles {
	
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
	 * Contains all needed file info to download all files
	 */
	private FileInfo[] fileInfo;
	
	private boolean isMetadata;

	/**
	 * Centralised storage of have pieces
	 */
	private Bitfield bitfield;

	/**
	 * Creates a Files instance based upon a .torrent file
	 * 
	 * @param torrentFile
	 */
	public Files(File torrentFile) {
		parseTorrentFileData(torrentFile);
		bitfield = new Bitfield(getBitfieldSize());
	}

	private void parseTorrentFileData(File torrentFile) {
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(torrentFile));
			byte[] data = new byte[in.available()];
			in.read(data, 0, data.length);
			in.close();
			Bencode decoder = new Bencode(new String(data));
			parseDictionary(decoder.decodeDictionary());
			isMetadata = false;
		} catch (IOException e) {
			e.printStackTrace();
			ThreadUtils.sleep(10);
			parseTorrentFileData(torrentFile);
		}
	}

	private void parseDictionary(HashMap<String, Object> dictionary) throws IOException {
		folderName = Config.getConfig().getString("download-output_folder") + dictionary.get("name");
		new File(folderName + "/").mkdirs();

		pieceSize = (int) dictionary.get("piece length");
		long remainingSize = 0L;

		if (dictionary.containsKey("files")) { // Multi-file torrent
			ArrayList<?> files = (ArrayList<?>) dictionary.get("files");
			fileInfo = new FileInfo[files.size()];
			for (int i = 0; i < fileInfo.length; i++) {
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
				int pieceCount = (int) JMath.ceilDivision(fileSize, pieceSize);
				if (remainingSize % pieceSize != 0 && fileSize >= pieceSize) {
					pieceCount++;
				}
				FileInfo info = new FileInfo(fileSize, remainingSize, getFile(fileName), pieceCount);
				fileInfo[i] = info;
				remainingSize += fileSize;
			}
		} else { // Single file torrent
			fileInfo = new FileInfo[1];
			String filename = (String) dictionary.get("name");
			long fileSize = getNumberFromDictionary(dictionary.get("length"));
			fileInfo[0] = new FileInfo(fileSize, remainingSize, getFile(filename), (int) JMath.ceilDivision(fileSize, pieceSize));
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
			l = (long) ((int) o);
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
	public void havePiece(int pieceIndex) {
		bitfield.havePiece(pieceIndex);
		if (!pieces.get(pieceIndex).isDone()) {
			for (int i = 0; i < pieces.get(pieceIndex).getBlockCount(); i++) {
				pieces.get(pieceIndex).setDone(i);
			}
		}
		long pieceOffset = pieceIndex * getPieceSize();
		long pieceEndOffset = pieceOffset + getPieceSize();
		for (int i = 0; i < fileInfo.length; i++) {
			FileInfo f = fileInfo[i];
			if (f.getFirstByteOffset() + f.getSize() >= pieceOffset && f.getFirstByteOffset() < pieceEndOffset) {
				f.addPiece(pieceIndex);
			}
		}
	}

	/**
	 * Checks if all files are downloaded
	 * 
	 * @return
	 */
	public boolean isDone() {
		if (isMetadata) {
			if (fileInfo[0].getSize() == 0L)
				return false;
		}
		return getNeededPieces().count() == 0;
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
	public long getPieceSize() {
		return pieceSize;
	}

	public int getBlockSize() {
		return BLOCK_SIZE;
	}

	/**
	 * Gets the FileInfo for the given piece and block
	 * 
	 * @param index The piece index
	 * @param blockIndex The block index within the piece
	 * @param blockDataOffset The offset within the block
	 * @return The FileInfo for the given data
	 */
	public FileInfo getFileForBlock(int index, int blockIndex, int blockDataOffset) throws TorrentException {
		long pieceOffset = (index * getPieceSize()) + (blockIndex * BLOCK_SIZE) + blockDataOffset;
		if (pieceOffset <= 0) {
			return fileInfo[0];
		} else {
			long fileTotal = 0L;
			for (int i = 0; i < fileInfo.length; i++) {
				fileTotal += fileInfo[i].getSize();
				if (pieceOffset < fileTotal) {
					return fileInfo[i];
				}
			}
			throw new TorrentException("Piece is not within any of the files");
		}
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
