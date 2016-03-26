package org.johnnei.javatorrent.internal.disk;

import java.util.PriorityQueue;

import org.johnnei.javatorrent.disk.IDiskJob;

public class IOManager implements Runnable {

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
	 * Processes all pending tasks.
	 *
	 */
	@Override
	public void run() {
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
