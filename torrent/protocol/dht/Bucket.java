package torrent.protocol.dht;


public class Bucket {
	
	/**
	 * The minimum allowed ID in this bucket
	 */
	private byte[] minId;
	/**
	 * The maximum allowed ID in this bucket
	 */
	private byte[] maxId;
	/**
	 * The timestamp at which the bucket has been updated
	 */
	private long lastChange;
	/**
	 * The data of the bucket which is at max 8 nodes
	 */
	private Node[] nodes;
	
	public Bucket(byte[] minId) {
		this.minId = minId;
		nodes = new Node[8];
	}
	
	/**
	 * Checks if the bucket is full
	 * @return true if the bucket contains 8 nodes
	 */
	public boolean isFull() {
		for(int i = 0; i < nodes.length; i++) {
			if(nodes[i] == null)
				return false;
		}
		return false;
	}
}
