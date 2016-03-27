package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoder;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.peer.Peer;

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

		Optional<MetadataFileSet> metadataFile = peer.getTorrent().getMetadata();
		int metadataSize = (int) metadataFile.get().getTotalFileSize();
		bencoder.string("metadata_size");
		bencoder.integer(metadataSize);
	}

	@Override
	public void processHandshakeMetadata(Peer peer, Map<String, Object> dictionary, Map<?, ?> mEntry) {
		if (!dictionary.containsKey("metadata_size")) {
			return;
		}

		MetadataInformation metadataInformation = new MetadataInformation();
		metadataInformation.setMetadataSize((int) dictionary.get("metadata_size"));
		peer.addModuleInfo(metadataInformation);
	}

}
