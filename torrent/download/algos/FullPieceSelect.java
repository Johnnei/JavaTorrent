package torrent.download.algos;

import java.util.ArrayList;
import java.util.Random;

import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.download.files.PieceInfo;
import torrent.download.peer.Peer;

/**
 * An basic improvement on {@link RandomSelect} RandomSelect.<br/>
 * Select a started piece, if none available: select the most available<br/>
 * Peer selection works the same as in {@link RandomSelect}
 * 
 * @author Johnnei
 * 
 */
public class FullPieceSelect implements IDownloadRegulator {

	private RandomSelect randSelect;
	private Random rand;
	private Torrent torrent;

	public FullPieceSelect(Torrent torrent) {
		this.torrent = torrent;
		rand = new Random();
		randSelect = new RandomSelect(torrent);
	}

	@Override
	public String getName() {
		return "Full Pieces first pass";
	}

	@Override
	public ArrayList<Peer> getPeerForPiece(PieceInfo p) {
		return randSelect.getPeerForPiece(p);
	}
	
	private PieceInfo getMostAvailable() {
		ArrayList<PieceInfo> undownloaded = torrent.getTorrentFiles().getUndownloadedPieces();
		int[] availability = new int[torrent.getTorrentFiles().getPieceCount()];
		int max = 0;
		PieceInfo info = null;
		for(PieceInfo pieceInfo : undownloaded) {
			ArrayList<Peer> peers = torrent.getDownloadableLeechers();
			for(Peer p : peers) {
				if(p.hasPiece(pieceInfo.getIndex()))
					availability[pieceInfo.getIndex()]++;
				if(availability[pieceInfo.getIndex()] > max) {
					max = availability[pieceInfo.getIndex()];
					info = pieceInfo;
				}
			}
		}
		if(info == null)
			return null;
		else {
			return info;
		}
	}

	@Override
	public PieceInfo getPiece() {
		ArrayList<PieceInfo> undownloaded = torrent.getTorrentFiles().getUndownloadedPieces();
		ArrayList<PieceInfo> started = new ArrayList<PieceInfo>();
		for (int i = 0; i < undownloaded.size(); i++) {
			PieceInfo info = undownloaded.get(i);
			Piece piece = torrent.getTorrentFiles().getPiece(info.getIndex());
			if (piece.isStarted() && !piece.isRequestedAll()) {
				started.add(info);
			}
		}
		if (started.size() < 10)
			return getMostAvailable();
		else
			return started.get(rand.nextInt(started.size()));
	}

}
