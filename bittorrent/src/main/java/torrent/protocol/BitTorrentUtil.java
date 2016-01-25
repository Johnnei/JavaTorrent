package torrent.protocol;

import java.io.IOException;

import org.johnnei.utils.JMath;

import torrent.download.AFiles;
import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.protocol.messages.MessageBitfield;
import torrent.protocol.messages.MessageHave;

public class BitTorrentUtil {

	/**
	 * Handles the sending of {@link IExtension} handshakes and sending of {@link MessageHave}/{@link MessageBitfield}
	 * @param peer
	 */
	public static void onPostHandshake(Peer peer) throws IOException {
		peer.getBitTorrentSocket().setPassedHandshake();
		sendHaveMessages(peer);
		peer.getTorrent().addPeer(peer);
	}

	private static void sendHaveMessages(Peer peer) throws IOException {
		if (peer.getTorrent().isDownloadingMetadata()) {
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
