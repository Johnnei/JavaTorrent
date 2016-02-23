package org.johnnei.javatorrent.disk;

import java.io.IOException;

public interface IDiskJob {

	int NORMAL = 0;
	int HIGH = 3;
	int CRITICAL = 10;

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
