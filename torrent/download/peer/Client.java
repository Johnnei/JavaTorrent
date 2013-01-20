package torrent.download.peer;

import java.util.HashMap;
import java.util.Set;

public class Client {

	private Bitfield bitfield;
	private boolean isChoked;
	private boolean isInterested;
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
	private int maxWorkQueue;

	public Client() {
		isChoked = true;
		isInterested = false;
		bitfield = new Bitfield();
		extentionIds = new HashMap<>();
		maxRequests = 20;
		maxWorkQueue = 1;
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

	public boolean isChoked() {
		return isChoked;
	}

	public boolean isInterested() {
		return isInterested;
	}
	
	public void setAbsoluteMaxRequests(int reqq) {
		maxRequests = reqq;
	}
	
	public void setMaxRequests(int i) {
		if(i > 0 && i <= maxRequests)
			maxWorkQueue = i;
	}
	
	/**
	 * Grabs the first job on the workingQueue
	 * @return
	 * The next job available
	 */
	public Job getNextJob() {
		return workingQueue.entrySet().iterator().next().getKey();
	}
	
	public int getMaxRequests() {
		return maxWorkQueue;
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
		workingQueue.remove(job);
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

	public void clearJobs() {
		workingQueue.clear();
	}
	
	public Bitfield getBitfield() {
		return bitfield;
	}

}
