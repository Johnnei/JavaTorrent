package org.johnnei.javatorrent.internal.torrent.peer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class Client {

	private final Object queueLock = new Object();

	private boolean isChoked;
	private boolean isInterested;

	/**
	 * The pieces to be send or be requested
	 */
	private Queue<Job> workingQueue;

	/**
	 * Creates a new Client.
	 */
	public Client() {
		isChoked = true;
		isInterested = false;
		workingQueue = new LinkedList<>();
	}

	/**
	 * Marks the client as choked.
	 */
	public void choke() {
		isChoked = true;
	}

	/**
	 * Marks the client as unchoked.
	 */
	public void unchoke() {
		isChoked = false;
	}

	/**
	 * Marks the client as interested.
	 */
	public void interested() {
		isInterested = true;
	}

	/**
	 * Marks the client as uninterested.
	 */
	public void uninterested() {
		isInterested = false;
	}

	/**
	 * Returns if the client is choked or not.
	 * When a client is Choked no data transfer is expected to happen
	 * @return <code>true</code> when the client is choked, otherwise <code>false</code>
	 */
	public boolean isChoked() {
		return isChoked;
	}

	/**
	 * Returns if the client is interested or not.
	 * When a client is interested it is expected to start requesting data when becoming unchoked.
	 * @return <code>true</code> when the client is interested, otherwise <code>false</code>
	 * @see #isChoked()
	 */
	public boolean isInterested() {
		return isInterested;
	}

	/**
	 * Grabs the first job on the workingQueue
	 *
	 * @return The next job available
	 */
	public Job popNextJob() {
		synchronized (queueLock) {
			return workingQueue.poll();
		}
	}

	/**
	 * Gets an iterable to allow iteration over the job list.
	 * @return An iteratable collection containing the jobs.
	 */
	public Iterable<Job> getJobs() {
		return Collections.unmodifiableCollection(workingQueue);
	}

	/**
	 * Removes a job from the working queue if it was listed
	 *
	 * @param job The job to remove
	 */
	public void removeJob(Job job) {
		synchronized (queueLock) {
			workingQueue.remove(job);
		}
	}

	/**
	 * Adds a job to the working queue
	 *
	 * @param job The job to add.
	 */
	public void addJob(Job job) {
		synchronized (queueLock) {
			workingQueue.add(job);
		}
	}

	/**
	 * The amount of jobs on the working queue
	 *
	 * @return the amount
	 */
	public int getQueueSize() {
		// The linkedlist implementation keeps track of the size, it can't throw an error on modification.
		return workingQueue.size();
	}

	/**
	 * Clears all jobs from the queue.
	 */
	public void clearJobs() {
		synchronized (queueLock) {
			workingQueue.clear();
		}
	}

}
