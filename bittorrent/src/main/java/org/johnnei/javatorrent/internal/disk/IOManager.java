package org.johnnei.javatorrent.internal.disk;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.internal.utils.Sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOManager implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(IOManager.class);

	private final Lock lock;

	private final Condition newTaskEvent;

	private PriorityQueue<DiskJobWrapper> taskQueue;

	public IOManager() {
		taskQueue = new PriorityQueue<>();
		lock = new ReentrantLock();
		newTaskEvent = lock.newCondition();
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
		Sync.signalAll(lock, newTaskEvent);

	}

	private boolean awaitTask() {
		while (taskQueue.isEmpty()) {
			try {
				lock.lock();
				newTaskEvent.await();
			} catch (InterruptedException e) {
				LOGGER.info("IO Manager was interrupted. Stopping thread.", e);
				return false;
			} finally {
				lock.unlock();
			}
		}

		return true;
	}

	private void processTasks() {
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

	/**
	 * Processes all pending tasks.
	 *
	 */
	@Override
	public void run() {
		if (!awaitTask()) {
			return;
		}

		processTasks();
	}

}
