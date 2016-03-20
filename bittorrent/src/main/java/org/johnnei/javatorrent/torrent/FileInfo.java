package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

public class FileInfo {

	/**
	 * The fileName
	 */
	private String fileName;

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
	private RandomAccessFile fileAccess;

	/**
	 * A lock to prevent concurrent writes to a single file
	 */
	public final Object FILE_LOCK = new Object();

	public FileInfo(long filesize, long firstByteOffset, File file, int pieceCount) {
		this.firstByteOffset = firstByteOffset;
		this.fileName = file.getName();
		this.filesize = filesize;
		this.pieceCount = pieceCount;
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			fileAccess = new RandomAccessFile(file, "rw");
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
	 * @return The size of this file.
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

	/**
	 * Gets the name of this file.
	 * @return The name of this file.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Gets the handle to write/read from this file.
	 * @return The IO handle.
	 */
	public RandomAccessFile getFileAccess() {
		return fileAccess;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}

		if (!(o instanceof FileInfo)) {
			return false;
		}

		FileInfo fileInfo = (FileInfo) o;
		return filesize == fileInfo.filesize && firstByteOffset == fileInfo.firstByteOffset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(filesize, firstByteOffset);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("FileInfo[firstByteOffset=%d, length=%d, name=%s]", firstByteOffset, filesize, fileName);
	}
}
