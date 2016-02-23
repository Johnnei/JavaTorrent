package org.johnnei.javatorrent.disk;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around the {@link IDiskJob} which records the amount of tries and defines the priority.
 */
public class DiskJobWrapper implements Comparable<DiskJobWrapper> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobWrapper.class);

	private final IDiskJob diskJob;

	private int attempt;

	public DiskJobWrapper(IDiskJob diskJob) {
		this.diskJob = diskJob;
	}

	/**
	 * Processes the wrapped diskjob.
	 * @return <code>true</code> when this job has been completed, otherwise <code>false</code>
	 */
	public boolean process() {
		try {
			diskJob.process();
			return true;
		} catch (IOException e) {
			attempt++;
			LOGGER.warn("Failed to process {}. (Attempt: {})", diskJob, attempt, e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(DiskJobWrapper o) {
		return diskJob.getPriority() - o.diskJob.getPriority();
	}
}
