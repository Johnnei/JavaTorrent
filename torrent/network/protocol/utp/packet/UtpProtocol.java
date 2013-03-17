package torrent.network.protocol.utp.packet;

public class UtpProtocol {

	public static final int ST_DATA = 0;
	public static final int ST_FIN = 1;
	public static final int ST_STATE = 2;
	public static final int ST_RESET = 3;
	public static final int ST_SYN = 4;
	
	public static final long getMicrotime() {
		return System.currentTimeMillis();
	}

}
