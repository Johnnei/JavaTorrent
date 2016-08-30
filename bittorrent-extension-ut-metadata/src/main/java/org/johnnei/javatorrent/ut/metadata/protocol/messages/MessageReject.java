package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;

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

		Optional<AbstractFileSet> optionalMetadata = peer.getTorrent().getMetadata().getFileSet();
		if (!optionalMetadata.isPresent()) {
			LOGGER.debug("Received ut_metadata reject even though we don't have the minimal metadata reference yet.");
			peer.getBitTorrentSocket().close();
			return;
		}

		Piece piece = optionalMetadata.get().getPiece(0);
		piece.setBlockStatus(blockIndex, BlockStatus.Needed);
		peer.onReceivedBlock(piece, blockIndex);
	}

	@Override
	public int getLength() {
		return bencodedData.length;
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
