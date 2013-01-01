package torrent;

import java.util.Random;

public class Manager {

	private static Manager manager = new Manager();

	public static Manager getManager() {
		return manager;
	}

	private int transactionId;
	private byte[] peerId;

	private Manager() {
		transactionId = new Random().nextInt();
		peerId = new byte[20];
		peerId[0] = '-';
		peerId[1] = 'J';
		peerId[2] = 'T';
		peerId[3] = '0';
		peerId[4] = '0';
		peerId[5] = '1';
		peerId[6] = '0';
		peerId[7] = '-';
		for (int i = 8; i < peerId.length; i++) {
			peerId[i] = (byte) (new Random().nextInt() & 0xFF);
		}
	}

	public synchronized int getNextTransactionId() {
		return transactionId++;
	}

	public byte[] getPeer() {
		return peerId;
	}

	public static byte[] getPeerId() {
		return getManager().getPeer();
	}

	public static int getTransactionId() {
		return getManager().getNextTransactionId();
	}

}
