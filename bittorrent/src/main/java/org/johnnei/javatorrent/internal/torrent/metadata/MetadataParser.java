package org.johnnei.javatorrent.internal.torrent.metadata;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedList;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedString;
import org.johnnei.javatorrent.bittorrent.encoding.IBencodedValue;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;

import static java.util.stream.Collectors.toList;

public class MetadataParser {

	private static final int SHA1_LENGTH_IN_BYTES = 20;

	public static void readMetadata(Metadata.Builder builder, IBencodedValue bencodedMetadata) {
		if (!(bencodedMetadata instanceof BencodedMap)) {
			throw new IllegalArgumentException("Metadata is expected to be a dictionary at the root");
		}

		BencodedMap metadata = (BencodedMap) bencodedMetadata;

		BencodedMap info = metadata.get("info")
			.filter(entry -> entry instanceof BencodedMap)
			.map(entry -> (BencodedMap) entry)
			.orElse(metadata);

		getName(info).ifPresent(builder::withName);

		int pieceSize = (int) (long) info.get("piece length")
			.filter(entry -> entry instanceof BencodedInteger)
			.map(IBencodedValue::asLong)
			.orElseThrow(() -> new IllegalArgumentException("Piece length is missing from info dictionary"));
		builder.withPieceSize(pieceSize);

		byte[] pieceHashes = info.get("pieces")
			.filter(entry -> entry instanceof BencodedString)
			.map(IBencodedValue::asBytes)
			.filter(entry -> entry.length % SHA1_LENGTH_IN_BYTES == 0)
			.orElseThrow(() -> new IllegalArgumentException("\"info\" entry is missing valid \"pieces\" entry."));

		int pieceCount = pieceHashes.length / SHA1_LENGTH_IN_BYTES;
		for (int index = 0; index < pieceCount; index++) {
			int hashOffset = index * SHA1_LENGTH_IN_BYTES;
			byte[] hash = new byte[SHA1_LENGTH_IN_BYTES];
			System.arraycopy(pieceHashes, hashOffset, hash, 0, hash.length);
			builder.withPieceHash(hash);
		}

		Optional<Long> lengthEntry = info.get("length")
			.filter(entry -> entry instanceof BencodedInteger)
			.map(IBencodedValue::asLong);

		Optional<List<BencodedMap>> filesEntry = info.get("files")
			.filter(entry -> entry instanceof BencodedList)
			.map(IBencodedValue::asList)
			.map(fileDictionaries ->
				fileDictionaries.stream()
					.filter(entry -> entry instanceof BencodedMap)
					.map(entry -> (BencodedMap) entry)
					.collect(toList()));

		if (lengthEntry.isPresent() && filesEntry.isPresent()) {
			throw new IllegalArgumentException("Conflicting metadata structure in info dictionary: both \"length\" and \"files\" entries exist");
		} else {
			lengthEntry.ifPresent(fileLength -> {
				String name = getName(info)
					.orElseThrow(() -> new IllegalArgumentException("\"name\" entry is missing from info dictionary for single file torrent"));

				FileEntry file = new FileEntry(name, fileLength, 0);
				builder.withFileEntry(file);
			});
			filesEntry.ifPresent(files -> {

				long byteOffset = 0;
				for (BencodedMap fileEntry : files) {
					String path = fileEntry.get("path")
						.filter(entry -> entry instanceof BencodedList)
						.map(IBencodedValue::asList)
						.filter(entry -> !entry.isEmpty())
						.stream()
						.flatMap(Collection::stream)
						.map(entry -> {
							if (entry instanceof BencodedString) {
								return entry.asString();
							} else {
								throw new IllegalArgumentException("non string element in file path");
							}
						})
						.map(Path::of)
						.reduce(Path::resolve)
						.orElseThrow(() -> new IllegalArgumentException("File entry does not have valid path"))
						.toFile()
						.getPath();

					long fileSize = fileEntry.get("length")
						.flatMap(entry -> {
							if (entry instanceof BencodedInteger) {
								return Optional.of(entry.asLong());
							} else {
								return Optional.empty();
							}
						}).orElseThrow(() -> new IllegalArgumentException("Missing \"length\" for file entry"));

					builder.withFileEntry(new FileEntry(path, fileSize, byteOffset));
					byteOffset += fileSize;
				}
			});
		}

	}

	private static Optional<String> getName(BencodedMap info) {
		return info.get("name")
			.filter(entry -> entry instanceof BencodedString)
			.map(IBencodedValue::asString);
	}
}
