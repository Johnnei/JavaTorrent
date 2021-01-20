package org.johnnei.javatorrent.torrent.fileset;

import java.util.Objects;

public final class FileEntry {
	/**
	 * The fileName
	 */
	private final String fileName;
	/**
	 * The filesize of this file in the torrent
	 */
	private final long filesize;
	/**
	 * The offset of the first byte crossed across all files
	 */
	private final long firstByteOffset;

	public FileEntry(String fileName, long filesize, long firstByteOffset) {
		this.fileName = fileName;
		this.filesize = filesize;
		this.firstByteOffset = firstByteOffset;
	}

	/**
	 * @return The size of this file.
	 */
	public long getSize() {
		return filesize;
	}

	/**
	 * @return The offset of the first byte as if the entire torrent is a single file
	 */
	public long getFirstByteOffset() {
		return firstByteOffset;
	}

	/**
	 * @return The name of this file.
	 */
	public String getFileName() {
		return fileName;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null) {
			return false;
		}

		if (!(o instanceof FileEntry)) {
			return false;
		}

		FileEntry fileEntry = (FileEntry) o;
		return filesize == fileEntry.filesize && firstByteOffset == fileEntry.firstByteOffset;
	}

	@Override
	public int hashCode() {
		return Objects.hash(filesize, firstByteOffset);
	}

	@Override
	public String toString() {
		return String.format("FileEntry[firstByteOffset=%d, length=%d, name=%s]", firstByteOffset, filesize, fileName);
	}
}
