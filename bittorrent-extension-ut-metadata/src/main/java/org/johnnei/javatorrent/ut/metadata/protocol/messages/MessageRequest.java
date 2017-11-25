package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import java.util.Optional;

import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageRequest extends AbstractMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageRequest.class);

	public MessageRequest() {
		/* Default constructor must be available to read this message */
	}

	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		// The ut_metadata defines each section as a piece, but internally we map them as a single torrent piece so we can re-use the logic.
		int blockIndex = (int) dictionary.get(PIECE_KEY).get().asLong();

		if (peer.getTorrent().isDownloadingMetadata()) {
			Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
			if (!peerExtensions.isPresent() || !peerExtensions.get().hasExtension(UTMetadata.NAME)) {
				LOGGER.warn("Request to send Metadata block to {} has been rejected. Peer doesn't know about UT_METADATA", peer);
				return;
			}

			MessageReject mr = new MessageReject(blockIndex);
			MessageExtension extendedMessage = new MessageExtension(peerExtensions.get().getExtensionId(UTMetadata.NAME), mr);
			peer.getBitTorrentSocket().enqueueMessage(extendedMessage);
		} else {
			Piece piece = peer.getTorrent().getMetadata().getFileSet().get().getPiece(0);

			peer.getTorrent().addDiskJob(new DiskJobReadBlock(
					piece,
					blockIndex * MetadataFileSet.BLOCK_SIZE,
					piece.getBlockSize(blockIndex),
					diskJob -> onReadMetadataBlockCompleted(peer, diskJob)));
		}
	}

	private void onReadMetadataBlockCompleted(Peer peer, DiskJobReadBlock readJob) {
		int blockIndex = readJob.getOffset() / MetadataFileSet.BLOCK_SIZE;
		Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
		if (!peerExtensions.isPresent() || !peerExtensions.get().hasExtension(UTMetadata.NAME)) {
			LOGGER.warn("Can't satisfy request to send Metadata block {} to {}. Missing ut_metadata id assignment for peer.", blockIndex, peer);
			peer.getBitTorrentSocket().close();
			return;
		}

		MessageData mData = new MessageData(blockIndex, readJob.getBlockData());
		MessageExtension extendedMessage = new MessageExtension(peerExtensions.get().getExtensionId(UTMetadata.NAME), mData);
		peer.getBitTorrentSocket().enqueueMessage(extendedMessage);
	}

	@Override
	public int getLength() {
		return bencodedData.length;
	}

	@Override
	public int getId() {
		return UTMetadata.REQUEST;
	}

	@Override
	public String toString() {
		return String.format("MessageRequest[piece=%s]", dictionary.get(PIECE_KEY));
	}

}
