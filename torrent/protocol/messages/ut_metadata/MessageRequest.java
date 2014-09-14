package torrent.protocol.messages.ut_metadata;

import torrent.download.Torrent;
import torrent.download.files.disk.DiskJobSendMetadataBlock;
import torrent.download.peer.Peer;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.extension.MessageExtension;

public class MessageRequest extends Message {

	public MessageRequest() {
	}

	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
			MessageReject mr = new MessageReject((int) dictionary.get("piece"));
			MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mr);
			peer.addToQueue(extendedMessage);
		} else {
			int piece = (int) dictionary.get("piece");
			peer.getTorrent().addDiskJob(new DiskJobSendMetadataBlock(peer, piece));
		}
	}

	@Override
	public int getLength() {
		return bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.REQUEST;
	}

	@Override
	public String toString() {
		return "UT_Metadata Request";
	}

}
