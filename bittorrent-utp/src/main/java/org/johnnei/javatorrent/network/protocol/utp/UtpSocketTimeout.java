package org.johnnei.javatorrent.network.protocol.utp;

public class UtpSocketTimeout extends Thread {

	public UtpSocketTimeout() {
		super("UtpSocket Timeout Checker");
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			UdpMultiplexer.getInstance().updateTimeout();
		}
	}

}
