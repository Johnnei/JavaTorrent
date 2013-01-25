package torrent.download.algos;

public interface IPeerManager {

	/**
	 * The maximum amount of peers allowed
	 * 
	 * @param torrentState
	 * @return
	 */
	public int getMaxPeers(byte torrentState);

	/**
	 * The maximum amount of peers allowed including those which are still handshaking/connecting
	 * 
	 * @param torrentState
	 * @return
	 */
	public int getMaxPendingPeers(byte torrentState);

	/**
	 * Returns the amount of peers we want to recieve from the tracker
	 * 
	 * @param torrentState The current torrent state
	 * @param connected The current amount of connected peers
	 * @return
	 */
	public int getAnnounceWantAmount(byte torrentState, int connected);

	/**
	 * Gets the name of the Peer Manager
	 * 
	 * @return
	 */
	public String getName();

}
