package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentProtocolViolationException;
import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class MessageRequest extends AbstractMessage {

	public MessageRequest() {
		/* Default constructor must be available to read this message */
	}

	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		// The ut_metadata defines each section as a piece, but internally we map them as a single torrent piece so we can re-use the logic.
		int blockIndex = (int) dictionary
			.get(PIECE_KEY)
			.orElseThrow(() -> new BitTorrentProtocolViolationException("Missing block index in request"))
			.asLong();

		int metadataExtensionId = peer.getModuleInfo(PeerExtensions.class)
			.filter(extensions -> extensions.hasExtension(UTMetadata.NAME))
			.map(extensions -> extensions.getExtensionId(UTMetadata.NAME))
			.orElseThrow(() -> new BitTorrentProtocolViolationException("Received metadata request without ut_metadata being registered for peer"));

		peer.getTorrent().getMetadata().getFileSet()
			.filter(AbstractFileSet::isDone)
			.ifPresentOrElse(
				metadataFileSet -> {
					Piece piece = metadataFileSet.getPiece(0);

					peer.getTorrent().addDiskJob(new DiskJobReadBlock(
						piece,
						blockIndex * MetadataFileSet.BLOCK_SIZE,
						piece.getBlockSize(blockIndex),
						diskJob -> onReadMetadataBlockCompleted(peer, metadataExtensionId, diskJob)));
				},
				() -> {
					MessageReject mr = new MessageReject(blockIndex);
					MessageExtension extendedMessage = new MessageExtension(metadataExtensionId, mr);
					peer.getBitTorrentSocket().enqueueMessage(extendedMessage);
				}
			);
	}

	private void onReadMetadataBlockCompleted(Peer peer, int metadataExtensionId, DiskJobReadBlock readJob) {
		int blockIndex = readJob.getOffset() / MetadataFileSet.BLOCK_SIZE;

		MessageData mData = new MessageData(blockIndex, readJob.getBlockData());
		MessageExtension extendedMessage = new MessageExtension(metadataExtensionId, mData);
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
