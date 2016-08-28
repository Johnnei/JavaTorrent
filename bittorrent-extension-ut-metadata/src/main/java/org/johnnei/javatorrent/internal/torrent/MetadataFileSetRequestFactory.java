package org.johnnei.javatorrent.internal.torrent;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;

/**
 * Implementation of {@link IFileSetRequestFactory} for {@link org.johnnei.javatorrent.torrent.MetadataFileSet}.
 */
public class MetadataFileSetRequestFactory implements IFileSetRequestFactory {

	@Override
	public IMessage createRequestFor(Piece piece, int byteOffset, int length) {
		return null;
	}

	@Override
	public IMessage createCancelRequestFor(Piece piece, int byteOffset, int length) {
		throw new UnsupportedOperationException("Metadata requests cannot be cancelled.");
	}

	@Override
	public boolean supportsCancellation() {
		return false;
	}
}
