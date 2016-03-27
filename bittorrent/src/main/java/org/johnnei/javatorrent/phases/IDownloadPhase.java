package org.johnnei.javatorrent.phases;

import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;

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

}
