package org.johnnei.javatorrent.internal.disk;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.johnnei.javatorrent.disk.IDiskJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around the {@link IDiskJob} which records the amount of tries and defines the priority.
 */
class DiskJobWrapper implements Comparable<DiskJobWrapper> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobWrapper.class);

	private static final Clock clock = Clock.systemDefaultZone();

	private final IDiskJob diskJob;

	private final Instant submitTime;

	private int attempt;

	DiskJobWrapper(IDiskJob diskJob) {
		this.diskJob = Objects.requireNonNull(diskJob, "Can't wrap a null-job");
		this.submitTime = clock.instant();
	}

	/**
	 * Processes the wrapped diskjob.
	 * @return <code>true</code> when this job has been completed, otherwise <code>false</code>
	 */
	public boolean process() {
		try {
			Instant startTime = clock.instant();
			diskJob.process();
			Instant runTime = clock.instant();
			LOGGER.trace("Processed {}. Wait Time: {}ms, Run Time: {}ns",
					diskJob,
					Duration.between(submitTime, startTime).toMillis(),
					Duration.between(startTime, runTime).toNanos());
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

	/**
	 * Tests this wrapper is wrapping the same job as the given object.
	 * @param obj The object to compare to.
	 * @return <code>true</code> when equal, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof DiskJobWrapper)) {
			return false;
		}

		DiskJobWrapper other = (DiskJobWrapper) obj;

		return Objects.equals(diskJob, other.diskJob);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("DiskJobWrapper[value=%s]", diskJob);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return diskJob.hashCode();
	}
}
