package torrent.download.algos;

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
	 * The follow-up phase<br/>
	 * If this phase {@link #isDone()} it should be updated to the {@link IDownloadPhase} given by this function
	 * @return
	 */
	public IDownloadPhase nextPhase();
	
	/**
	 * Processing the phase of the torrent
	 */
	public void process();
	
	/**
	 * Prepare the phase
	 */
	public void preprocess();
	
	/**
	 * Clean up the phase
	 */
	public void postprocess();
	
	/**
	 * The number identifying this phase
	 * @return
	 */
	public byte getId();
}
