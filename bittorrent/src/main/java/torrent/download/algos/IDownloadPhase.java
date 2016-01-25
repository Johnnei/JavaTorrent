package torrent.download.algos;

import java.util.Collection;

import torrent.download.peer.Peer;

/**
 * A phase in the download which will be regulated by this phase
 * @author Johnnei
 *
 */
public interface IDownloadPhase {

	/**
	 * Checks if the phase has been finished
	 * @return true if the phase can be ended
	 */
	public boolean isDone();

	/**
	 * Processing the phase of the torrent
	 */
	public void process();

	/**
	 * Prepare the phase
	 */
	public void onPhaseEnter();

	/**
	 * Clean up the phase
	 */
	public void onPhaseExit();

	/**
	 * Filters the connected peer to just those which can benefit us in this phase.
	 * @param peers The list of available peers
	 * @return The list of peers which are useful.
	 */
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers);

}
