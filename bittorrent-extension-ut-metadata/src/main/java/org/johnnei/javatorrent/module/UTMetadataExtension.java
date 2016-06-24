package org.johnnei.javatorrent.module;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut_metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.ut_metadata.protocol.messages.MessageData;
import org.johnnei.javatorrent.ut_metadata.protocol.messages.MessageReject;
import org.johnnei.javatorrent.ut_metadata.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.ut_metadata.protocol.messages.MessageUnknown;
import org.johnnei.javatorrent.utils.Argument;

public class UTMetadataExtension implements IExtension {

	private File torrentFileFolder;

	private File downloadFolder;

	public UTMetadataExtension(File torrentFileFolder, File downloadFolder) {
		this.torrentFileFolder = Argument.requireNonNull(torrentFileFolder, "Torrent file folder must be configured.");
		this.downloadFolder = Argument.requireNonNull(downloadFolder, "Download folder must be configured.");
	}

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
			message = new MessageUnknown();
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
	public void addHandshakeMetadata(Peer peer, BencodedMap bencoder) {
		if (peer.getTorrent().isDownloadingMetadata()) {
			return;
		}

		Optional<MetadataFileSet> metadataFile = peer.getTorrent().getMetadata();
		int metadataSize = (int) metadataFile.get().getTotalFileSize();
		bencoder.put("metadata_size", new BencodedInteger(metadataSize));
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

	/**
	 * Gets the location of the torrent file for the given torrent.
	 * @param torrent The torrent for which the metadata file location is requested.
	 * @return The metadata file location for the given torrent.
	 */
	public File getTorrentFile(Torrent torrent) {
		return new File(torrentFileFolder, String.format("%s.torrent", torrent.getHash().toLowerCase()));
	}

	/**
	 * Returns the root of the download folder.
	 * @return The download folder.
	 */
	public File getDownloadFolder() {
		return downloadFolder;
	}

}
