package org.johnnei.javatorrent.internal.disk;

import java.util.PriorityQueue;

import org.johnnei.javatorrent.disk.IDiskJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOManager implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(IOManager.class);

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
		while (!taskQueue.isEmpty()) {
			DiskJobWrapper task;
			synchronized (this) {
				task = taskQueue.remove();
			}

			LOGGER.trace("Processing task: {}", task);

			if (!task.process()) {
				synchronized (this) {
					taskQueue.add(task);
				}
			}
		}
	}

}
