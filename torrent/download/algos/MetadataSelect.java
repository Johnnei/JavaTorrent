package torrent.download.algos;

import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.download.peer.Peer;

public class MetadataSelect implements IDownloadRegulator {
	
	private Torrent torrent;
	
	public MetadataSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	public String getName() {
		return "Full Piece Select (Metadata)";
	}

	@Override
	public Piece getPieceForPeer(Peer p) {
		Piece piece = torrent.getFiles().getPiece(0);
		if (piece.getRequestedCount() < piece.getBlockCount()) {
			return piece;
		}
		
		return null;
	}

}
