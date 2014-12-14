package torrent.protocol;

import java.io.IOException;

import org.johnnei.utils.JMath;

import torrent.download.AFiles;
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
		peer.getBitTorrentSocket().setPassedHandshake();
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
					//peer.getTorrent().getFiles().getMetadataSize()
					0 // TODO Correctly implement the metadata size again.
				)
			);
		}
		
		peer.getBitTorrentSocket().queueMessage(message);
	}
	
	private static void sendHaveMessages(Peer peer) throws IOException {
		if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
			return;
		}
		
		Torrent torrent = peer.getTorrent();
		AFiles files = torrent.getFiles();
		
		if (files.countCompletedPieces() == 0) {
			return;
		}
		
		if (JMath.ceilDivision(torrent.getFiles().getPieceCount(), 8) + 1 < 5 * files.countCompletedPieces()) {
			peer.getBitTorrentSocket().queueMessage(new MessageBitfield(files.getBitfieldBytes()));
		} else {
			for (int pieceIndex = 0; pieceIndex < torrent.getFiles().getPieceCount(); pieceIndex++) {
				if (!torrent.getFiles().hasPiece(pieceIndex)) {
					continue;
				}
				
				peer.getBitTorrentSocket().queueMessage(new MessageHave(pieceIndex));
			}
		}
	}

}
