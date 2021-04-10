package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.encoding.IBencodedValue;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.internal.torrent.metadata.MetadataParser;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.StringUtils;

/**
 * Represents the .torrent file of a {@link Torrent}
 */
public class Metadata {

	private static final int METADATA_FILE_HASH_SIZE = 20;

	private static final int PIECE_HASH_SIZE = 20;

	private final AbstractFileSet fileSet;

	private final byte[] btihHash;

	private final List<FileEntry> fileEntries;

	private final List<byte[]> pieceHashes;

	private final long pieceSize;

	private final String name;

	private Metadata(Builder builder) {
		btihHash = builder.btihHash;
		this.fileEntries = new ArrayList<>(builder.fileEntries);
		this.pieceHashes = new ArrayList<>(builder.pieceHashes);

		if (builder.name == null) {
			this.name = String.format("magnet(%s)", StringUtils.byteArrayToString(btihHash));
		} else {
			this.name = builder.name;
		}

		this.pieceSize = builder.pieceSize;
		this.fileSet = builder.fileSet;
	}

	public String getName() {
		return name;
	}

	public List<FileEntry> getFileEntries() {
		return fileEntries;
	}

	public List<byte[]> getPieceHashes() {
		return pieceHashes;
	}

	public long getPieceSize() {
		return pieceSize;
	}

	/**
	 * Gets the 20 byte BTIH hash of the torrent.
	 * @return The 20 byte BTIH hash.
	 */
	public byte[] getHash() {
		return Arrays.copyOf(btihHash, btihHash.length);
	}

	/**
	 * Gets the {@link #getHash()} formatted as hexadecimal.
	 * @return The BTIH hash in hexadecimal.
	 *
	 * @see #getHash()
	 */
	public String getHashString() {
		return StringUtils.byteArrayToString(btihHash);
	}

	public Optional<AbstractFileSet> getFileSet() {
		return Optional.ofNullable(fileSet);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Metadata)) {
			return false;
		}

		Metadata metadata = (Metadata) o;
		return Arrays.equals(btihHash, metadata.btihHash);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(btihHash);
	}

	public static class Builder {

		private final byte[] btihHash;

		private final List<FileEntry> fileEntries;

		private final List<byte[]> pieceHashes;

		private AbstractFileSet fileSet;

		private long pieceSize;

		private String name;

		public Builder(Metadata original) {
			this.btihHash = original.btihHash;
			this.fileEntries = original.fileEntries;
			this.pieceHashes = original.pieceHashes;
			this.fileSet = original.fileSet;
			this.pieceSize = original.pieceSize;
			this.name = original.name;
		}

		public Builder(byte[] btihHash) {
			if (btihHash.length != METADATA_FILE_HASH_SIZE) {
				throw new IllegalArgumentException("Metadata file hash must be exactly " + METADATA_FILE_HASH_SIZE + " bytes");
			}
			this.btihHash = btihHash;
			fileEntries = new LinkedList<>();
			pieceHashes = new LinkedList<>();
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withFileEntry(FileEntry entry) {
			this.fileEntries.add(entry);
			return this;
		}

		public Builder withPieceHash(byte[] pieceHash) {
			if (pieceHash.length != PIECE_HASH_SIZE) {
				throw new IllegalArgumentException("Piece hash must be exactly " + PIECE_HASH_SIZE + " bytes");
			}

			this.pieceHashes.add(pieceHash);
			return this;
		}

		public Builder withPieceSize(int pieceSize) {
			if (pieceSize <= 0) {
				throw new IllegalArgumentException("Piece size must be > 0");
			}

			this.pieceSize = pieceSize;
			return this;
		}

		public Builder withFileSet(AbstractFileSet fileSet) {
			this.fileSet = fileSet;
			return this;
		}

		public Metadata build() {
			return new Metadata(this);
		}

		public static Builder from(Path metadataFile) throws IOException {
			byte[] metadataBytes = Files.readAllBytes(metadataFile);
			Builder builder = new Builder(SHA1.hash(metadataBytes));

			Bencoding bencoder = new Bencoding();
			IBencodedValue metadataDictionary = bencoder.decode(new InStream(metadataBytes));
			MetadataParser.readMetadata(builder, metadataDictionary);

			return builder;
		}

	}
}
