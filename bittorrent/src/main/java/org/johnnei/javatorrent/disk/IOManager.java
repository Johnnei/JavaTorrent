package org.johnnei.javatorrent.disk;

import java.util.PriorityQueue;

import org.johnnei.javatorrent.torrent.Torrent;

public class IOManager {

	private PriorityQueue<DiskJob> taskQueue;

	public IOManager() {
		taskQueue = new PriorityQueue<>();
	}

	/**
	 * Adds a task to the queue
	 * 
	 * @param task The task to add
	 */
	public void addTask(DiskJob task) {
		synchronized (this) {
			taskQueue.add(task);
		}
	}

	/**
	 * Processes all pending tasks for the given torrent
	 * 
	 * @param torrent
	 */
	public void processTask(Torrent torrent) {
		while (taskQueue.size() > 0) {
			DiskJob task;
			synchronized (this) {
				task = taskQueue.remove();
			}
			task.process(torrent);
		}
	}

}
