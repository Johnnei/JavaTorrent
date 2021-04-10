package org.johnnei.javatorrent.ut.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.encoding.IBencodedValue;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.internal.torrent.metadata.MetadataParser;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;

public class UtMetadata {

	public static Metadata.Builder from(Path metadataFile) throws IOException {
		byte[] metadataBytes = Files.readAllBytes(metadataFile);
		byte[] hash = SHA1.hash(metadataBytes);
		Metadata.Builder builder = new Metadata.Builder(hash);

		Bencoding bencoder = new Bencoding();
		IBencodedValue metadataDictionary = bencoder.decode(new InStream(metadataBytes));
		MetadataParser.readMetadata(builder, metadataDictionary);

		MetadataFileSet fileSet = new MetadataFileSet(hash, metadataFile);
		Piece piece = fileSet.getPiece(0);
		int blocks = piece.getBlockCount();
		for (int index = 0; index < blocks; index++) {
			piece.setBlockStatus(index, BlockStatus.Verified);
		}

		return builder.withFileSet(fileSet);
	}

}
