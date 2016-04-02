package org.johnnei.javatorrent.internal.disk;

/**
 * A set of default priorities
 */
public enum DiskJobPriority {

	RECEIVED_DATA(0),
	OUTGOING_DATA(10),
	LOCAL_ACTION(3);

	private final int priority;

	DiskJobPriority(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}
