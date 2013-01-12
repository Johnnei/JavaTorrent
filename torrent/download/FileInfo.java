package torrent.download;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileInfo {

	/**
	 * The filename
	 */
	private String filename;
	/**
	 * The filesize of this file in the torrent
	 */
	private long filesize;
	/**
	 * The index of this file in the torrent
	 */
	private int index;
	/**
	 * The amount of pieces that have been downloaded
	 */
	private int pieceHaveCount;
	/**
	 * The offset of the first byte crossed across all files
	 */
	private long firstByteOffset;
	/**
	 * The link between the file on the harddrive
	 */
	private RandomAccessFile fileAcces;
	/**
	 * A lock to prevent concurrent writes to a single file
	 */
	public final Object FILE_LOCK = new Object();

	public FileInfo(int index, String filename, long filesize, long firstByteOffset, File file) {
		this.index = index;
		this.firstByteOffset = firstByteOffset;
		this.filename = filename;
		this.filesize = filesize;
		pieceHaveCount = 0;
		try {
			if(!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			fileAcces = new RandomAccessFile(file, "rw");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void addPiece() {
		pieceHaveCount++;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * Gets the size of this file
	 * @return
	 */
	public long getSize() {
		return filesize;
	}

	public int getPieceHaveCount() {
		return pieceHaveCount;
	}
	
	/**
	 * The offset of the first byte as if the entire torrent is a single file
	 * @return the first byte offset
	 */
	public long getFirstByteOffset() {
		return firstByteOffset;
	}

	public String getFilename() {
		return filename;
	}

	public RandomAccessFile getFileAcces() {
		return fileAcces;
	}

}
