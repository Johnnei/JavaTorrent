package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.johnnei.javatorrent.torrent.fileset.FileEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileInfo {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileInfo.class);

	private final FileEntry fileEntry;

	/**
	 * The amount of pieces which contain a part of data for this file
	 */
	private int pieceCount;

	/**
	 * The link between the file on the harddrive
	 */
	private RandomAccessFile fileAccess;

	/**
	 * A lock to prevent concurrent writes to a single file
	 */
	public final Object fileLock = new Object();

	public FileInfo(long filesize, long firstByteOffset, File file, int pieceCount) {
		this.fileEntry = new FileEntry(file.getName(), filesize, firstByteOffset);
		this.pieceCount = pieceCount;
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			fileAccess = new RandomAccessFile(file, "rw");
		} catch (IOException ex) {
			LOGGER.warn("Failed to open read/write access to {}", file.getAbsolutePath(), ex);
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
		return fileEntry.getSize();
	}

	/**
	 * The offset of the first byte as if the entire torrent is a single file
	 *
	 * @return the first byte offset
	 */
	public long getFirstByteOffset() {
		return fileEntry.getFirstByteOffset();
	}

	/**
	 * Gets the name of this file.
	 * @return The name of this file.
	 */
	public String getFileName() {
		return fileEntry.getFileName();
	}

	/**
	 * Gets the handle to write/read from this file.
	 * @return The IO handle.
	 */
	public RandomAccessFile getFileAccess() {
		return fileAccess;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (!(o instanceof FileInfo)) {
			return false;
		}

		return fileEntry.equals(((FileInfo) o).fileEntry);
	}

	@Override
	public int hashCode() {
		return fileEntry.hashCode();
	}

	@Override
	public String toString() {
		return String.format("FileInfo[entry=%s, pieceCount=%d]", fileEntry.toString(), pieceCount);
	}
}
