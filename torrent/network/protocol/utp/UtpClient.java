package torrent.network.protocol.utp;

public class UtpClient {

	/**
	 * The connection id on which this Client is Sending/Receiving Packets
	 */
	private short connectionId;
	
	/**
	 * The base delay<Br/>
	 * This is used as a correction so we can measure extra delay
	 */
	private long baseDelay;
	
	/**
	 * The delay corrected with {@link #baseDelay}
	 */
	private long delay;
	/**
	 * The maximum amount of bytes in flight<br/>
	 * Bytes are in flight when they are send but not yet acked
	 */
	private long windowSize;
	
	public UtpClient() {
		baseDelay = Long.MAX_VALUE;
		windowSize = 150;
	}

	/**
	 * Gets the delay measured on this socket
	 * @return The measured delay
	 */
	public int getDelay() {
		return (int)(delay & 0xFFFFFFFF);
	}
	
	public void setDelay(long delay, boolean updateBaseDelay) {
		if(updateBaseDelay && delay < baseDelay) {
			baseDelay = delay;
		}
		this.delay = delay - baseDelay;
	}

	/**
	 * Updates the measured delay and corrects the base delay if needed
	 * @param delay The measured delay
	 */
	public void setDelay(long delay) {
		setDelay(delay, true);
	}
	
	public void setConnectionId(int connectionId) {
		this.connectionId = (short)connectionId;
	}
	
	/**
	 * Sets the window to the maximum of the given size or 150
	 * @param size the new size
	 */
	public void setWindowSize(long size) {
		windowSize = Math.max(150, size);
	}
	
	public int getConnectionId() {
		return connectionId & 0xFFFF;
	}
	
	/**
	 * Gets the unsigned int for the window size
	 * @return
	 */
	public int getWindowSize() {
		return (int)(windowSize & 0xFFFFFFFF);
	}
}
