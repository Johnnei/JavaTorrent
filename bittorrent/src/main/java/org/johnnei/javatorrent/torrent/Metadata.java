package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedList;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.encoding.IBencodedValue;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.Argument;
import org.johnnei.javatorrent.utils.StringUtils;

/**
 * Represents the .torrent file of a {@link Torrent}
 */
public class Metadata {

	private static final String ERR_INCOMPLETE_INFO_ENTRY
			= "Metadata file appears to be validly encoded but is missing critical information from the 'info' entry.";

	private AbstractFileSet fileSet;

	private final byte[] btihHash;

	private List<FileEntry> fileEntries;

	private List<byte[]> pieceHashes;

	private long pieceSize;

	private String name;

	private Metadata(Builder builder) {
		btihHash = Argument.requireNonNull(builder.btihHash, "info hash must be supplied.");
		
		if (builder.metadataStructure == null) {
			fileEntries = Collections.emptyList();
			pieceHashes = Collections.emptyList();
		} else {
			parseMetadataStructure(builder.metadataStructure);
		}
	}

	public void setFileSet(AbstractFileSet fileSet) {
		if (this.fileSet != null) {
			throw new IllegalStateException("Cannot replace the metadata fileset once set.");
		}

		this.fileSet = fileSet;
	}

	public void initializeMetadata(byte[] buffer) {
		if (!fileEntries.isEmpty()) {
			throw new IllegalStateException("Cannot re-initialize metadata.");
		}

		if (!Arrays.equals(btihHash, SHA1.hash(buffer))) {
			throw new IllegalArgumentException("Data in buffer did not match the torrent hash");
		}

		BencodedMap metadataInfo = (BencodedMap) new Bencoding().decode(new InStream(buffer));
		parseMetadataStructure(metadataInfo);
	}

	private void parseMetadataStructure(BencodedMap metadataInfo) {
		fileEntries = new ArrayList<>();
		pieceHashes = new ArrayList<>();

		if (!isInfoDirectory(metadataInfo.asMap())) {
			metadataInfo = (BencodedMap) metadataInfo.get("info").orElseThrow(() -> new IllegalArgumentException(ERR_INCOMPLETE_INFO_ENTRY));

			if (!isInfoDirectory(metadataInfo.asMap())) {
				throw new IllegalArgumentException(ERR_INCOMPLETE_INFO_ENTRY);
			}
		}

		parseDictionary(metadataInfo);
	}

	private void parseDictionary(BencodedMap dictionary) {
		dictionary.get("name").ifPresent(name -> this.name = name.asString());

		// Because we got here the test in isInfoDirectory have passed, the size is available.
		pieceSize = (int) dictionary.get("piece length").get().asLong();

		Optional<IBencodedValue> filesEntry = dictionary.get("files");
		if (filesEntry.isPresent()) {
			// Multi-file torrent
			List<IBencodedValue> files = filesEntry.get().asList();
			long byteOffset = 0L;
			for (IBencodedValue fileEntry : files) {
				BencodedMap file = (BencodedMap) fileEntry;
				long fileSize = file.get("length").get().asLong();
				BencodedList fileStructure = (BencodedList) file.get("path").get();
				String fileName = "";
				if (fileStructure.size() > 1) {
					for (int j = 0; j < fileStructure.size(); j++) {
						fileName += "/" + fileStructure.get(j);
					}
				} else {
					fileName = fileStructure.get(0).asString();
				}
				FileEntry info = new FileEntry(fileName, fileSize, byteOffset);
				fileEntries.add(info);
				byteOffset += fileSize;
			}
		} else {
			// Single file torrent
			String filename = dictionary.get("name").get().asString();
			long fileSize = dictionary.get("length").get().asLong();
			fileEntries.add(new FileEntry(filename, fileSize, 0));
		}

		byte[] hashBytes = dictionary.get("pieces").get().asBytes();
		int pieceAmount = hashBytes.length / 20;
		for (int index = 0; index < pieceAmount; index++) {
			int hashOffset = index * 20;
			byte[] sha1Hash = new byte[20];
			System.arraycopy(hashBytes, hashOffset, sha1Hash, 0, sha1Hash.length);
			pieceHashes.add(sha1Hash);
		}
	}

	private boolean isInfoDirectory(Map<String, IBencodedValue> metadata) {
		if (!metadata.containsKey("length") && !metadata.containsKey("files")) {
			return false;
		}

		if (!metadata.containsKey("pieces")) {
			return false;
		}

		if (!metadata.containsKey("piece length")) {
			return false;
		}

		return true;
	}

	public String getName() {
		if (name == null) {
			return String.format("magnet(%s)", getHashString());
		}

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

		private final Bencoding bencoding;

		private byte[] btihHash;
		
		private BencodedMap metadataStructure;

		public Builder() {
			bencoding = new Bencoding();
		}

		public Builder setHash(byte[] btihHash) {
			this.btihHash = btihHash;
			return this;
		}

		public Builder readFromFile(File metadataFile) throws IOException {
			try (ByteInputStream in = new ByteInputStream(new FileInputStream(metadataFile))) {
				byte[] buffer = new byte[(int) metadataFile.length()];
				in.readFully(buffer);
				readFromByteArray(buffer);
			}

			return this;
		}

		public Builder readFromByteArray(byte[] buffer) {
			btihHash = SHA1.hash(buffer);
			metadataStructure = (BencodedMap) bencoding.decode(new InStream(buffer));

			return this;
		}
		
		public Metadata build() {
			return new Metadata(this);
		}

	}
}
