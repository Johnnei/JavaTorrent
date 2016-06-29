package org.johnnei.javatorrent.disk;

import java.io.IOException;

public interface IDiskJob {

	/**
	 * Processes the disk job
	 */
	void process() throws IOException;

	/**
	 * The priority of this job
	 *
	 * @return The priority
	 */
	int getPriority();

}
