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
				int pieceCount = (int) JMath.ceilDivision(fileSize, pieceSize);
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
			fileInfos.add(new FileInfo(fileSize, remainingSize, getFile(filename), (int) JMath.ceilDivision(fileSize, pieceSize)));
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
		for (FileInfo f : fileInfos) {
			if (f.getFirstByteOffset() + f.getSize() >= pieceOffset && f.getFirstByteOffset() < pieceEndOffset) {
				f.addPiece(pieceIndex);
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
	 * @param pieceIndex The piece index
	 * @param blockIndex The block index within the piece
	 * @param byteOffset The offset within the block
	 * @return The FileInfo for the given data
	 */
	public FileInfo getFileForBytes(int pieceIndex, int blockIndex, int byteOffset) throws TorrentException {
		long bytesStartPosition = (pieceIndex * getPieceSize()) + (blockIndex * BLOCK_SIZE) + byteOffset;
		
		if (bytesStartPosition < 0) {
			throw new TorrentException("Trying to find file for bytes with a negative byte start position.");
		}
		
		for (FileInfo fileInfo : fileInfos) {
			// If the file started before or at the wanted byteStartPosition then that file contains the bytes
			if (fileInfo.getFirstByteOffset() <= bytesStartPosition) {
				return fileInfo;
			}
		}
		
		throw new TorrentException("Piece is not within any of the files");
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
