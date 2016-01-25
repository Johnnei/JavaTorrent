package torrent.download.algos;

import java.util.Collection;

import torrent.download.Torrent;
import torrent.download.peer.Peer;

public class PhaseUpload implements IDownloadPhase {

	private Torrent torrent;

	public PhaseUpload(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPhaseEnter() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPhaseExit() {
		torrent.getLogger().info("Upload target reached");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		// TODO Auto-generated method stub
		return null;
	}

}
