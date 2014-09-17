package torrent.protocol;

import java.io.IOException;
import java.util.Collection;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.protocol.messages.MessageBitfield;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.extension.MessageHandshake;

public class BitTorrentUtil {
	
	/**
	 * Handles the sending of {@link IExtension} handshakes and sending of {@link MessageHave}/{@link MessageBitfield}
	 * @param peer
	 */
	public static void onPostHandshake(Peer peer) throws IOException {
		if (peer.getExtensions().hasExtension(5, 0x10)) {
			// Extended Messages extension
			sendExtendedMessages(peer);
		}
		
		sendHaveMessages(peer);
		peer.getTorrent().addPeer(peer);
	}
	
	private static void sendExtendedMessages(Peer peer) throws IOException {
		MessageExtension message;
		
		if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
			message = new MessageExtension(
				BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, 
				new MessageHandshake()
			);
		} else {
			message = new MessageExtension(
				BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, 
				new MessageHandshake(
					peer.getTorrent().getFiles().getMetadataSize()
				)
			);
		}
		
		peer.getBitTorrentSocket().queueMessage(message);
	}
	
	private static void sendHaveMessages(Peer peer) throws IOException {
		if (peer.getTorrent().getDownloadStatus() != Torrent.STATE_DOWNLOAD_DATA) {
			return;
		}
		
		Collection<IMessage> messages = peer.getTorrent().getFiles().getBitfield().getBitfieldMessage();
		for (IMessage message : messages) {
			peer.getBitTorrentSocket().queueMessage(message);
		}
	}

}
