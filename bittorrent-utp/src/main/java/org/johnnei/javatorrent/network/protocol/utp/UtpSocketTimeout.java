package org.johnnei.javatorrent.network.protocol.utp;

import org.johnnei.javatorrent.utils.ThreadUtils;

public class UtpSocketTimeout extends Thread {

	public UtpSocketTimeout() {
		super("UtpSocket Timeout Checker");
	}

	@Override
	public void run() {
		while (true) {
			ThreadUtils.sleep(250);
			UdpMultiplexer.getInstance().updateTimeout();
		}
	}

}
