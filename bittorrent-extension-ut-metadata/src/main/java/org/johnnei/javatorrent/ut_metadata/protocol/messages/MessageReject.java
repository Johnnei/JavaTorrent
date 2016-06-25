package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.util.Optional;

import org.johnnei.javatorrent.ut_metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageReject extends AbstractMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageReject.class);

	public MessageReject() {
		super();
	}

	public MessageReject(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		int blockIndex = (int) dictionary.get(PIECE_KEY).get().asLong();
		LOGGER.warn("Piece Request got rejected: " + blockIndex);

		Optional<MetadataFileSet> optionalMetadata = peer.getTorrent().getMetadata();
		if (!optionalMetadata.isPresent()) {
			LOGGER.debug("Received ut_metadata reject even though we don't have the minimal metadata reference yet.");
			peer.getBitTorrentSocket().close();
			return;
		}

		optionalMetadata.get().getPiece(0).setBlockStatus(blockIndex, BlockStatus.Needed);
		peer.onReceivedBlock(0, blockIndex);
	}

	@Override
	public int getLength() {
		return bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.REJECT;
	}

	@Override
	public String toString() {
		return String.format("MessageReject[piece=%s]", dictionary.get(PIECE_KEY));
	}

}
