package org.johnnei.javatorrent.internal.torrent;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;

/**
 * Implementation of {@link IFileSetRequestFactory} for {@link org.johnnei.javatorrent.torrent.TorrentFileSet}
 */
public class TorrentFileSetRequestFactory implements IFileSetRequestFactory {

	@Override
	public IMessage createRequestFor(Piece piece, int byteOffset, int length) {
		return new MessageRequest(piece.getIndex(), byteOffset, length);
	}

	@Override
	public IMessage createCancelRequestFor(Piece piece, int byteOffset, int length) {
		return new MessageCancel(piece.getIndex(), byteOffset, length);
	}

	@Override
	public boolean supportsCancellation() {
		return true;
	}
}
