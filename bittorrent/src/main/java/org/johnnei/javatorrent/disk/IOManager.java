package org.johnnei.javatorrent.disk;

import java.util.PriorityQueue;

public class IOManager {

	private PriorityQueue<DiskJobWrapper> taskQueue;

	public IOManager() {
		taskQueue = new PriorityQueue<>();
	}

	/**
	 * Adds a task to the queue
	 * 
	 * @param task The task to add
	 */
	public void addTask(IDiskJob task) {
		synchronized (this) {
			taskQueue.add(new DiskJobWrapper(task));
		}
	}

	/**
	 * Processes all pending tasks for the given torrent
	 * 
	 */
	public void processTask() {
		while (taskQueue.size() > 0) {
			DiskJobWrapper task;
			synchronized (this) {
				task = taskQueue.remove();
			}

			if (!task.process()) {
				synchronized (this) {
					taskQueue.add(task);
				}
			}
		}
	}

}
