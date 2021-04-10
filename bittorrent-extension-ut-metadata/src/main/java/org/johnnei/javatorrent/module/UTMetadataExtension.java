package org.johnnei.javatorrent.module;

import java.nio.file.Path;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageData;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageReject;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageUnknown;
import org.johnnei.javatorrent.utils.Argument;

/**
 * The {@link IExtension} which adds support for the ut_metadata extension (BEP #9).
 */
public class UTMetadataExtension implements IExtension {

	private Bencoding bencoding = new Bencoding();

	private Path torrentFileFolder;

	private Path downloadFolder;

	/**
	 * Creates a new instance of the UT Metadata extension.
	 * @param torrentFileFolder The folder for the torrent to download the torrent file into.
	 * @param downloadFolder The folder for the torrent to download the data into.
	 */
	public UTMetadataExtension(Path torrentFileFolder, Path downloadFolder) {
		this.torrentFileFolder = Argument.requireNonNull(torrentFileFolder, "Torrent file folder must be configured.");
		this.downloadFolder = Argument.requireNonNull(downloadFolder, "Download folder must be configured.");
	}

	@Override
	public IMessage getMessage(InStream inStream) {
		inStream.mark();

		// Decode on a copy so we can have two mark states.
		BencodedMap dictionary = (BencodedMap) bencoding.decode(new InStream(inStream.readFully(inStream.available())));
		int id = (int) dictionary.get("msg_type").orElseThrow(() -> new IllegalArgumentException("Missing msg_type in ut_metadata message.")).asLong();
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

		inStream.resetToMark();
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

		Optional<AbstractFileSet> metadataFile = peer.getTorrent().getMetadata().getFileSet();
		// TODO(Johnnei): Ensure that the MetadataRequests work when the metadata phases aren't configured.
		// There is a window (and a valid path when the phases aren't configured) where ut_metadata is not able to supply the metadata size.
		metadataFile.ifPresent(fileSet -> {
			int metadataSize = (int) fileSet.getTotalFileSize();
			bencoder.put("metadata_size", new BencodedInteger(metadataSize));
		});
	}

	@Override
	public void processHandshakeMetadata(Peer peer, BencodedMap dictionary, BencodedMap mEntry) {
		dictionary.get("metadata_size").ifPresent(metadataSize -> {
			MetadataInformation metadataInformation = new MetadataInformation();
			metadataInformation.setMetadataSize(metadataSize.asLong());
			peer.addModuleInfo(metadataInformation);
		});
	}

	/**
	 * Gets the location of the torrent file for the given torrent.
	 * @param torrent The torrent for which the metadata file location is requested.
	 * @return The metadata file location for the given torrent.
	 */
	public Path getTorrentFile(Torrent torrent) {
		return torrentFileFolder.resolve(
			String.format("%s.torrent", torrent.getMetadata().getHashString().toLowerCase())
		);
	}

	/**
	 * Returns the root of the download folder.
	 * @return The download folder.
	 */
	public Path getDownloadFolder() {
		return downloadFolder;
	}

}
