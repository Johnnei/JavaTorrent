package org.johnnei.javatorrent.torrent.download;

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
	 * The amount of pieces which contain a part of data for this file
	 */
	private int pieceCount;
	
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

	public FileInfo(long filesize, long firstByteOffset, File file, int pieceCount) {
		this.firstByteOffset = firstByteOffset;
		this.filename = file.getName();
		this.filesize = filesize;
		this.pieceCount = pieceCount;
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			fileAcces = new RandomAccessFile(file, "rw");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * The amount of pieces which contain a part of data for this file
	 * 
	 * @return the amount of pieces for this file
	 */
	public int getPieceCount() {
		return pieceCount;
	}

	/**
	 * Gets the size of this file
	 * 
	 * @return
	 */
	public long getSize() {
		return filesize;
	}

	/**
	 * The offset of the first byte as if the entire torrent is a single file
	 * 
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
