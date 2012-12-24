package torrent.download.peer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import torrent.network.PieceRequest;

public class Client {

	/**
	 * The pieces the other client has available
	 */
	private HashMap<Integer, Boolean> pieces;
	private boolean isChoked;
	private boolean isInterested;
	private ArrayList<PieceRequest> requestedPieces;
	private byte[] reserved_bytes;
	private HashMap<String, Integer> extentionIds;
	/**
	 * The pieces to be send or be requested
	 */
	private HashMap<Job, Integer> workingQueue;
	/**
	 * The number of outstanding request messages this client supports without dropping any.<br/>
	 * Extension Protocol item: dictionary["reqq"]
	 */
	private int maxRequests;

	public Client() {
		isChoked = true;
		isInterested = false;
		pieces = new HashMap<Integer, Boolean>();
		requestedPieces = new ArrayList<PieceRequest>();
		extentionIds = new HashMap<>();
		maxRequests = 1;
		workingQueue = new HashMap<>();
	}

	public void setReservedBytes(byte[] l) {
		this.reserved_bytes = l;
	}

	public void choke() {
		isChoked = true;
	}

	public void unchoke() {
		isChoked = false;
	}

	public void interested() {
		isInterested = true;
	}

	public void uninterested() {
		isInterested = false;
	}

	public void addPiece(int i) {
		if (pieces.get(i) == null)
			pieces.put(i, true);
	}

	public boolean isChoked() {
		return isChoked;
	}

	public boolean isInterested() {
		return isInterested;
	}

	public boolean hasPiece(int index) {
		return pieces.get(index) != null;
	}

	public int hasPieceCount() {
		return pieces.size();
	}

	public void requesting(PieceRequest pr) {
		requestedPieces.add(pr);
	}
	
	public void setMaxRequests(int i) {
		maxRequests = i;
	}
	
	public int getMaxRequests() {
		return maxRequests;
	}

	public void addExtentionID(String key, int id) {
		extentionIds.put(key, id);
	}

	public int getExtentionID(String key) {
		return extentionIds.get(key);
	}

	public boolean supportsExtention(int index, int bit) {
		return (reserved_bytes[index] & bit) > 0;
	}

	public boolean hasExtentionID(String extention) {
		return extentionIds.containsKey(extention);
	}
	
	public Set<Job> getKeySet() {
		return workingQueue.keySet();
	}
	
	/**
	 * Removes a job from the working queue if it was listed
	 * @param job
	 */
	public void removeJob(Job job) {
		
	}
	
	/**
	 * Adds a job to the working queue
	 * @param job
	 */
	public void addJob(Job job) {
		workingQueue.put(job, 0);
	}
	
	/**
	 * The amount of jobs on the working queue
	 * @return
	 * the amount
	 */
	public int getQueueSize() {
		return workingQueue.size();
	}

}
