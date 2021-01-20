package org.johnnei.javatorrent.phases;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.peer.Peer;

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
	boolean isDone();

	/**
	 * Processing the phase of the torrent
	 */
	void process();

	/**
	 * Prepare the phase
	 */
	void onPhaseEnter();

	/**
	 * Clean up the phase
	 */
	void onPhaseExit();

	/**
	 * Gets the choking strategy which is optimal for this phase.
	 * @return The optimal choking strategy.
	 */
	IChokingStrategy getChokingStrategy();

	/**
	 * @param peer The peer to check
	 * @return <code>true</code> when the peer supports all the required protocols for this phase
	 */
	boolean isPeerSupportedForDownload(Peer peer);

	Optional<AbstractFileSet> getFileSet();

	PiecePrioritizer getPiecePrioritizer();

}
