package torrent.network.utp;

public class UtpClient {

	/**
	 * The connectionId for this client
	 */
	private int connectionId;
	/**
	 * The maximum amount of bytes in flight for this connection
	 */
	private int windowSize;
	/**
	 * The measured delay on this connection
	 */
	private int delay;
	/**
	 * The delay correction
	 */
	private int delayCorrection;
	
	public UtpClient() {
		windowSize = 150;
		connectionId = UtpSocket.NO_CONNECTION;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getDelay() {
		return delay;
	}

	public int getDelayCorrection() {
		return delayCorrection;
	}

	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	public void setWindowSize(int window_size) {
		this.windowSize = window_size;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public void setDelayCorrection(int delay_base) {
		this.delayCorrection = delay_base;
	}
}
