package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.download.algos.PhasePreMetadata;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.download.MetadataFile;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.encoding.Bencode;
import org.johnnei.javatorrent.torrent.encoding.Bencoder;

public class UTMetadataExtension implements IExtension {

	@Override
	public IMessage getMessage(InStream inStream) {
		int moveBackLength = inStream.available();

		Bencode decoder = new Bencode(inStream.readString(inStream.available()));
		Map<String, Object> dictionary = decoder.decodeDictionary();
		int id = (int) dictionary.get("msg_type");
		IMessage message;
		switch (id) {
		case UTMetadata.DATA:
			message = new MessageData();
			break;

		case UTMetadata.REJECT:
			message = new MessageReject();
			break;

		case UTMetadata.REQUEST:
			message = new MessageRequest();
			break;

		default:
			message = null;
			break;
		}

		inStream.moveBack(moveBackLength);
		return message;

	}

	@Override
	public String getExtensionName() {
		return UTMetadata.NAME;
	}

	@Override
	public void addHandshakeMetadata(Peer peer, Bencoder bencoder) {
		if (peer.getTorrent().isDownloadingMetadata()) {
			return;
		}

		Optional<MetadataFile> metadataFile = peer.getTorrent().getMetadata();
		int metadataSize = (int) metadataFile.get().getTotalFileSize();
		bencoder.string("metadata_size");
		bencoder.integer(metadataSize);
	}

	@Override
	public void processHandshakeMetadata(Peer peer, Map<String, Object> dictionary, Map<?, ?> mEntry) {
		if (!dictionary.containsKey("metadata_size")) {
			return;
		}

		IDownloadPhase phase = peer.getTorrent().getDownloadPhase();
		if (phase instanceof PhasePreMetadata) {
			PhasePreMetadata preMetaData = (PhasePreMetadata) phase;
			preMetaData.setMetadataSize((int) dictionary.get("metadata_size"));
		}
	}

}
