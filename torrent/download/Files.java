package torrent.download;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;
import org.johnnei.utils.config.DefaultConfig;

import torrent.TorrentException;
import torrent.download.files.HashedPiece;
import torrent.download.files.Piece;
import torrent.download.peer.Bitfield;
import torrent.encoding.Bencode;

public class Files {

	/**
	 * The pieces downloaded by this torrent
	 */
	private HashedPiece[] pieces;
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
	private FileInfo metadata;
	private long totalSize;
	private boolean isMetadata;
	/**
	 * The default size for a block
	 */
	private int blockSize;
	/**
	 * Centralised storage of have pieces
	 */
	private Bitfield bitfield;
	
	/**
	 * Creates a Files instance based upon a magnet-link
	 * @param filename
	 */
	public Files(String filename) {
		isMetadata = true;
		blockSize = 16384;
		fileInfo = new FileInfo[1];
		fileInfo[0] = new FileInfo(0, filename, 0L, 0L, new File(Config.getConfig().getTempFolder() + filename));
		pieces = new HashedPiece[0];
		bitfield = new Bitfield(getBitfieldSize());
	}

	/**
	 * Creates a Files instance based upon a .torrent file
	 * @param torrentFile
	 */
	public Files(File torrentFile) {
		blockSize = 1 << 14;
		metadata = new FileInfo(0, torrentFile.getName(), torrentFile.length(), 0, torrentFile);
		parseTorrentFileData(torrentFile);
		bitfield = new Bitfield(getBitfieldSize());
	}

	private void parseTorrentFileData(File torrentFile) {
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(torrentFile));
			String data = "";
			while(in.available() > 0)
				data += (char)in.readByte();
			in.close();
			Bencode decoder = new Bencode(data);
			parseDictionary(decoder.decodeDictionary());
			isMetadata = false;
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			parseTorrentFileData(torrentFile);
		}
	}

	private void parseDictionary(HashMap<String, Object> dictionary) throws IOException {
		folderName = Config.getConfig().getString("download-output_folder", DefaultConfig.DOWNLOAD_OUTPUT_FOLDER) + dictionary.get("name");
		new File(folderName + "/").mkdirs();

		pieceSize = (int) dictionary.get("piece length");
		long remainingSize = 0L;

		if (dictionary.containsKey("files")) { // Multi-file torrent
			ArrayList<?> files = (ArrayList<?>) dictionary.get("files");
			fileInfo = new FileInfo[files.size()];
			for (int i = 0; i < fileInfo.length; i++) {
				HashMap<?, ?> file = (HashMap<?, ?>) files.get(i);
				long fileSize = 0L;
				Object o = file.get("length");
				if (o instanceof Integer) {
					fileSize = (long) ((int) o);
				} else {
					fileSize = (long) o;
				}
				ArrayList<?> fileStructure = (ArrayList<?>) file.get("path");
				String fileName = "";
				if (fileStructure.size() > 1) {
					for (int j = 0; j < fileStructure.size(); j++) {
						fileName += "/" + fileStructure.get(j);
					}
				} else {
					fileName = (String) fileStructure.get(0);
				}
				FileInfo info = new FileInfo(i, fileName, fileSize, remainingSize, getFile(fileName));
				fileInfo[i] = info;
				remainingSize += fileSize;
			}
		} else { // Single file torrent

		}
		totalSize = remainingSize;
		String pieceHashes = (String) dictionary.get("pieces");
		int pieceAmount = pieceHashes.length() / 20;
		pieces = new HashedPiece[pieceAmount];
		for (int index = 0; index < pieceAmount; index++) {
			int hashOffset = index * 20;
			int size = (remainingSize >= pieceSize) ? pieceSize : (int) remainingSize;
			byte[] sha1Hash = new byte[20];
			char[] hashBytes = pieceHashes.substring(hashOffset, hashOffset + 20).toCharArray();
			for(int i = 0; i < sha1Hash.length; i++) {
				sha1Hash[i] = (byte)hashBytes[i];
			}
			pieces[index] = new HashedPiece(sha1Hash, this, index, size, blockSize);
			remainingSize -= size;
		}
	}
	
	/**
	 * Adds the have to the bitfield and updates the correct fileInfo have counts
	 * @param pieceIndex
	 */
	public void havePiece(int pieceIndex) {
		bitfield.havePiece(pieceIndex);
		if(!pieces[pieceIndex].isDone()) {
			for(int i = 0; i < pieces[pieceIndex].getBlockCount(); i++) {
				pieces[pieceIndex].setDone(i);
			}
		}
		long pieceOffset = pieceIndex * getPieceSize();
		long pieceEndOffset = pieceOffset + getPieceSize();
		for(int i = 0; i < fileInfo.length; i++) {
			FileInfo f = fileInfo[i];
			if(f.getFirstByteOffset() + f.getSize() >= pieceOffset && f.getFirstByteOffset() < pieceEndOffset) {
				f.addPiece();
			}
		}
	}

	/**
	 * Checks if all files are downloaded
	 * @return
	 */
	public boolean isDone() {
		if(isMetadata) {
			if(fileInfo[0].getSize() == 0L)
				return false;
		}
		return getNeededPieces().size() == 0;
	}
	
	public int getBitfieldSize() {
		return (int)Math.ceil(pieces.length / 8D);
	}

	/**
	 * Gets the proper file location for the given filename
	 * @param name The desired file name
	 * @return The file within the download folder
	 */
	private File getFile(String name) {
		return new File(folderName + "/" + name);
	}

	public HashedPiece getPiece(int index) {
		return pieces[index];
	}

	public File getFolderName() {
		return new File(folderName);
	}
	
	public Bitfield getBitfield() {
		return bitfield;
	}

	/**
	 * Gets the default piece size
	 * @return The default piece size
	 */
	public long getPieceSize() {
		return pieceSize;
	}

	/**
	 * Returns the list of pieces which are not completed yet
	 * @return
	 */
	public ArrayList<Piece> getNeededPieces() {
		ArrayList<Piece> undownloaded = new ArrayList<>();
		for(int i = 0; i < pieces.length; i++) {
			if(!pieces[i].isDone()) {
				undownloaded.add(pieces[i]);
			}
		}
		return undownloaded;
	}

	/**
	 * The amount of pieces in all pieces
	 * @return
	 */
	public int getPieceCount() {
		return pieces.length;
	}

	/**
	 * The total size of all pieces together
	 * @return
	 */
	public long getTotalSize() {
		return totalSize;
	}

	public FileInfo[] getFiles() {
		return fileInfo;
	}

	public int getBlockIndexByOffset(int offset) {
		return offset / blockSize;
	}
	
	public int getBlockSize() {
		return blockSize;
	}
	
	/**
	 * The amount of bytes still needed to be downloaded
	 * @return
	 */
	public long getRemainingBytes() {
		long left = 0;
		for(int i = 0; i < pieces.length; i++) {
			left += pieces[i].getRemainingBytes();
		}
		return left;
	}

	/**
	 * Gets the FileInfo for the given piece and block
	 * @param index The piece index
	 * @param blockIndex The block index within the piece
	 * @param blockDataOffset The offset within the block
	 * @return The FileInfo for the given data
	 */
	public FileInfo getFileForBlock(int index, int blockIndex, int blockDataOffset) throws TorrentException {
		long pieceOffset = (index * getPieceSize()) + (blockIndex * blockSize) + blockDataOffset;
		if(pieceOffset <= 0) {
			return fileInfo[0];
		} else {
			long fileTotal = 0L;
			for(int i = 0; i < fileInfo.length; i++) {
				fileTotal += fileInfo[i].getSize();
				if(pieceOffset < fileTotal) {
					return fileInfo[i];
				}
			}
			throw new TorrentException("Piece is not within any of the files");
		}
	}
	
	/**
	 * Sets the size of the metadata file  
	 * @param torrentHash the associated torrent hash
	 * @param size the size of the metadata file
	 */
	public void setFilesize(byte[] torrentHash, int size) {
		if(isMetadata && fileInfo[0].getSize() == 0L && size > 0) {
			pieceSize = size;
			pieces = new HashedPiece[] { new HashedPiece(torrentHash, this, 0, size, blockSize) };
			FileInfo f = fileInfo[0];
			fileInfo[0] = new FileInfo(0, f.getFilename(), size, 0, new File(Config.getConfig().getTempFolder() + f.getFilename()));
			totalSize = size;
			bitfield = new Bitfield(1);
		}
	}

	/**
	 * checks if the files are metadata
	 * @return
	 */
	public boolean isMetadata() {
		return isMetadata;
	}

	/**
	 * Gets a block from the metadata file
	 * @param piece The block
	 * @return
	 * The 16384 bytes needed to answer the request
	 */
	public byte[] getMetadataBlock(int piece) {
		long blockSize = 16384L;
		byte[] data = new byte[(int)blockSize];
		long blockOffset = piece * blockSize;
		synchronized (metadata.FILE_LOCK) {
			RandomAccessFile fileAccess = metadata.getFileAcces();
			try {
				if(fileAccess.length() < blockOffset + data.length)
					data = new byte[(int)(fileAccess.length() - blockOffset)];
				int bytesRead = 0;
				while(bytesRead < data.length) {
					fileAccess.seek(piece * blockSize + bytesRead);
					bytesRead += fileAccess.read(data, piece * (int)blockSize + bytesRead, data.length - bytesRead);
				}
			} catch (IOException e) {
			}
		}
		return data;
	}

	/**
	 * Gets the size of the metadata file
	 * @return
	 */
	public long getMetadataSize() {
		return metadata.getSize();
	}
}
