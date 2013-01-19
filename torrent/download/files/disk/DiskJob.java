package torrent.download.files.disk;

import torrent.download.Torrent;

public abstract class DiskJob implements Comparable<DiskJob> {
	
	public static final int NORMAL = 0;
	public static final int HIGH = 3;
	public static final int CRITICAL = 10;
	
	/**
	 * Processes the disk job
	 */
	public abstract void process(Torrent torrent);
	
	/**
	 * The priority of this job
	 * @return
	 * The priority
	 */
	public abstract int getPriority();
	
	@Override
	public int compareTo(DiskJob job) {
		return getPriority() - job.getPriority();
	}

}
