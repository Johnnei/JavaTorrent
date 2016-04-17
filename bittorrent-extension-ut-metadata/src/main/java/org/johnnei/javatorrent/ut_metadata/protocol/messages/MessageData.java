package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import java.util.Optional;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.ut_metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageData extends AbstractMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageData.class);

	private byte[] data;

	public MessageData() {
		// Default constructor for when the message gets received.
	}

	public MessageData(int piece, byte[] data) {
		super(piece);
		this.data = data;
	}

	@Override
	public void write(OutStream outStream) {
		super.write(outStream);
		outStream.write(data);
	}

	@Override
	public void read(InStream inStream) {
		super.read(inStream);
		data = inStream.readFully(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		int blockIndex = (int) dictionary.get(PIECE_KEY);

		if (!peer.getTorrent().isDownloadingMetadata()) {
			LOGGER.debug("Peer {} sent ut_metadata block but we already got all metadata info. Ignoring.", peer);
			return;
		}

		Optional<MetadataFileSet> metadataFileSet = peer.getTorrent().getMetadata();
		if (!metadataFileSet.isPresent()) {
			LOGGER.warn("Peer {} send ut_metadata block to us but we don't know the size of the metadata yet.", peer);
			peer.getBitTorrentSocket().close();
			return;
		}

		peer.getTorrent().onReceivedBlock(metadataFileSet.get(), 0, blockIndex * metadataFileSet.get().getBlockSize(), data);
	}

	@Override
	public int getLength() {
		return data.length + bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.DATA;
	}

	@Override
	public String toString() {
		return String.format("MessageData[piece=%s]", dictionary.get(PIECE_KEY));
	}

}
