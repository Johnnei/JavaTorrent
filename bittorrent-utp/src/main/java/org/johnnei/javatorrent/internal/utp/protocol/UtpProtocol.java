package org.johnnei.javatorrent.internal.utp.protocol;

public class UtpProtocol {

	public static final int VERSION = 1;

	public static final int ST_DATA = 0;
	public static final int ST_FIN = 1;
	public static final int ST_STATE = 2;
	public static final int ST_RESET = 3;
	public static final int ST_SYN = 4;

	private UtpProtocol() {
		// No utility classes for you!
	}

}
