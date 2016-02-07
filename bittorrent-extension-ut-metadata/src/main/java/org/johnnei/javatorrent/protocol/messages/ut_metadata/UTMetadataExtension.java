package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.download.algos.PhasePreMetadata;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.IExtension;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.torrent.download.MetadataFile;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.encoding.Bencode;
import org.johnnei.javatorrent.torrent.encoding.Bencoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UTMetadataExtension implements IExtension {

	private static final Logger LOGGER = LoggerFactory.getLogger(UTMetadataExtension.class);

	@Override
	public IMessage getMessage(InStream inStream) {
		int moveBackLength = inStream.available();

		try {
			Bencode decoder = new Bencode(inStream.readString(inStream.available()));
			HashMap<String, Object> dictionary = decoder.decodeDictionary();
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
		} catch (InvalidObjectException e) {
			LOGGER.warn("Received incorrect message", e);
			return null;
		}
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
	public void processHandshakeMetadata(Peer peer, HashMap<String, Object> dictionary, Map<?, ?> mEntry) {
		if (!mEntry.containsKey(UTMetadata.NAME)) {
			return;
		}

		peer.getExtensions().register(UTMetadata.NAME, (Integer) mEntry.get(UTMetadata.NAME));
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
