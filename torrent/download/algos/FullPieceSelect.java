package torrent.download.algos;

import java.util.ArrayList;
import java.util.Random;

import torrent.download.Torrent;
import torrent.download.files.Piece;
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
	public ArrayList<Peer> getPeerForPiece(Piece p) {
		return randSelect.getPeerForPiece(p);
	}
	
	private Piece getMostAvailable() {
		ArrayList<Piece> undownloaded = torrent.getFiles().getNeededPieces();
		int[] availability = new int[torrent.getFiles().getPieceCount()];
		int max = 0;
		Piece info = null;
		for(Piece pieceInfo : undownloaded) {
			ArrayList<Peer> peers = torrent.getDownloadablePeers();
			for(Peer p : peers) {
				if(p.getClient().hasPiece(pieceInfo.getIndex()))
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
	public Piece getPiece() {
		ArrayList<Piece> undownloaded = torrent.getFiles().getNeededPieces();
		ArrayList<Piece> started = new ArrayList<Piece>();
		for (int i = 0; i < undownloaded.size(); i++) {
			Piece info = undownloaded.get(i);
			Piece piece = torrent.getFiles().getPiece(info.getIndex());
			if (piece.isStarted() && piece.getRequestedCount() < piece.getBlockCount()) {
				started.add(info);
			}
		}
		if (started.size() < 20)
			return getMostAvailable();
		else
			return started.get(rand.nextInt(started.size()));
	}

}
