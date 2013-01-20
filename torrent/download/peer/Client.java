package torrent.download.peer;

import java.util.HashMap;
import java.util.Set;

import org.johnnei.utils.JMath;

public class Client {

	private byte[] bitfield;
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
		bitfield = new byte[0];
		extentionIds = new HashMap<>();
		maxRequests = 20;
		maxWorkQueue = 1;
		workingQueue = new HashMap<>();
	}
	
	/**
	 * Increases or decreased the bitfield size but it will preserve the old data
	 * @param size The new size to grow/shrink to
	 */
	public void setBitfieldSize(int size) {
		if(size != bitfield.length) {
			byte[] newBitfield = new byte[size];
			int maxSize = JMath.max(size, bitfield.length);
			for(int i = 0; i < maxSize; i++) {
				newBitfield[i] = bitfield[i];
			}
			bitfield = newBitfield;
		}
	}
	
	/**
	 * Override bitfield with a given one
	 * @param bitfield The new bitfield
	 */
	public void setBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
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
	
	/**
	 * Checks the bitfield if we have the given piece
	 * @param pieceIndex the piece to check
	 * @return True if we verified the hash of that piece, else false
	 */
	public boolean hasPiece(int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		return ((bitfield[byteIndex] >> bit) & 1) == 1;
	}
	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * @param pieceIndex The piece to add
	 */
	public void havePiece(int pieceIndex) {
		havePiece(pieceIndex, false);
	}
	
	/**
	 * Notify that we have the given piece<br/>
	 * This will update the bitfield to bitwise OR the bit to 1
	 * @param pieceIndex The piece to add
	 * @param mayExpand If the bitfield may grow to fit the new have data
	 */
	public void havePiece(int pieceIndex, boolean mayExpand) {
		int byteIndex = pieceIndex / 8;
		int bit = pieceIndex % 8;
		if(bitfield.length < byteIndex) {
			if(mayExpand) {
				byte[] newBitfield = new byte[byteIndex + 1];
				for(int i = 0; i < bitfield.length; i++) {
					newBitfield[i] = bitfield[i];
				}
				this.bitfield = newBitfield;
			} else {
				return; //Prevent IndexOutOfRange
			}
		}
		bitfield[byteIndex] |= (1 << bit);
	}

	/**
	 * Goes through the bitfield and checks how many pieces the client has
	 * @return The amount of pieces the client has
	 */
	public int hasPieceCount() {
		int have = 0;
		for(int i = 0; i < bitfield.length; i++) {
			byte b = bitfield[i];
			for (int bit = 7; bit > 0; bit--) {
				if (((b >> bit) & 1) == 1) {
					++have;
				}
			}
		}
		return have;
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

}
