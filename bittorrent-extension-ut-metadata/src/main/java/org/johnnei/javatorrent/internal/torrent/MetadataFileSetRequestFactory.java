package org.johnnei.javatorrent.internal.torrent;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageRequest;

/**
 * Implementation of {@link IFileSetRequestFactory} for {@link org.johnnei.javatorrent.torrent.MetadataFileSet}.
 */
public class MetadataFileSetRequestFactory implements IFileSetRequestFactory {

	@Override
	public IMessage createRequestFor(Peer peer, Piece piece, int byteOffset, int length) {
		PeerExtensions peerExtensions = peer.getModuleInfo(PeerExtensions.class)
				.orElseThrow(() -> new TorrentException("Requesting metadata block from peer which doesn't support ut_metadata"));
		return new MessageExtension(peerExtensions.getExtensionId(UTMetadata.NAME), new MessageRequest(byteOffset / MetadataFileSet.BLOCK_SIZE));
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
