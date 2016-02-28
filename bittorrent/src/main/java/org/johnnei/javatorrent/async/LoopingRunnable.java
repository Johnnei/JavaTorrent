package org.johnnei.javatorrent.async;

/**
 * A {@link Runnable} wrapper which handles the stopping of a runnable which should be looping.
 */
public class LoopingRunnable implements Runnable {

	private final Runnable runnable;

	private boolean keepRunning;

	public LoopingRunnable(Runnable runnable) {
		this.runnable = runnable;
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
		}
	}

}
