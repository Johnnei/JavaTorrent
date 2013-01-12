package torrent.download.algos;

import java.util.ArrayList;
import java.util.Random;

import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.download.peer.Peer;

/**
 * A Download Regulator which random selects pieces to be downloaded The pieces will be attached to the first peer which has it in the list
 * 
 * @author Johnnei
 * 
 */
public class RandomSelect implements IDownloadRegulator {

	private Torrent torrent;
	private Random rand;

	public RandomSelect(Torrent torrent) {
		this.torrent = torrent;
		rand = new Random();
	}

	@Override
	public String getName() {
		return "Random First to pass";
	}

	@Override
	public ArrayList<Peer> getPeerForPiece(Piece pieceInfo) {
		ArrayList<Peer> peerList = torrent.getDownloadablePeers();
		ArrayList<Peer> resultList = new ArrayList<Peer>();
		for (int i = 0; i < peerList.size(); i++) {
			Peer p = peerList.get(i);
			if (p.isWorking() && p.getFreeWorkTime() == 0)
				continue;
			if (p.getClient().hasPiece(pieceInfo.getIndex())) {
				resultList.add(p);
			}
		}
		return resultList;
	}

	@Override
	public Piece getPiece() {
		ArrayList<Piece> undownloaded = torrent.getFiles().getNeededPieces();
		if (undownloaded.size() == 0)
			return null;
		return undownloaded.get(rand.nextInt(undownloaded.size()));
	}

}
