package org.johnnei.javatorrent.torrent.network.protocol.utp.packet;

public class UtpProtocol {

	public static final int ST_DATA = 0;
	public static final int ST_FIN = 1;
	public static final int ST_STATE = 2;
	public static final int ST_RESET = 3;
	public static final int ST_SYN = 4;
	
	/**
	 * Using nanoseconds/1000, The official implementation notes:<br/>
	 * "This should return monotonically increasing microseconds, start point does not matter"<br/>
	 * Therefore it shouldn't matter if our starting point is different
	 * @return a monotonically increasing microsecond timestamp
	 */
	public static final int getMicrotime() {
		return (int)(System.nanoTime() / 1000);
	}

}
