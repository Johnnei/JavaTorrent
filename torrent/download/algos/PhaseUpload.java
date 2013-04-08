package torrent.download.algos;

import torrent.download.Torrent;

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
	public IDownloadPhase nextPhase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preprocess() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postprocess() {
		torrent.log("Upload target reached");
	}

	@Override
	public byte getId() {
		return Torrent.STATE_UPLOAD;
	}

}
