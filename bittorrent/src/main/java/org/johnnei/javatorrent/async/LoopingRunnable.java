package org.johnnei.javatorrent.async;

import org.johnnei.javatorrent.utils.Argument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Runnable} wrapper which handles the stopping of a runnable which should be looping.
 */
public class LoopingRunnable implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoopingRunnable.class);

	private final Runnable runnable;

	private final boolean isEventBased;

	private volatile boolean keepRunning;

	public LoopingRunnable(Runnable runnable) {
		this(runnable, false);
	}

	public LoopingRunnable(Runnable runnable, boolean isEventBased) {
		this.runnable = Argument.requireNonNull(runnable, "Runnable not cannot be null");
		this.isEventBased = isEventBased;
		keepRunning = true;
	}

	/**
	 * Stops the runnable once it has completed its cycle.
	 */
	public void stop() {
		keepRunning = false;
	}

	@Override
	public void run() {
		while (keepRunning) {
			runnable.run();

			if (!isEventBased) {
				throttleThread();
			}
		}
	}

	private void throttleThread() {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			LOGGER.trace("Ignoring interrupted exception for endless looping task.", e);
			Thread.currentThread().interrupt();
		}
	}

}
