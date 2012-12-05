package torrent.download.algos;

import java.util.ArrayList;
import java.util.Random;

import torrent.download.Torrent;
import torrent.download.files.PieceInfo;
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
	public ArrayList<Peer> getPeerForPiece(PieceInfo pieceInfo) {
		ArrayList<Peer> peerList = new ArrayList<Peer>();
		for (int i = 0; i < torrent.getPeers().size(); i++) {
			Peer p = torrent.getPeers().get(i);
			if (p.isWorking() && p.getFreeWorkTime() == 0)
				continue;
			if (p.getMyClient().isChoked())
				continue;
			if (p.hasPiece(pieceInfo.getIndex())) {
				peerList.add(p);
			}
		}
		return peerList;
	}

	@Override
	public PieceInfo getPiece() {
		ArrayList<PieceInfo> undownloaded = torrent.getTorrentFiles().getUndownloadedPieces();
		if (undownloaded.size() == 0)
			return null;
		return undownloaded.get(rand.nextInt(undownloaded.size()));
	}

}
