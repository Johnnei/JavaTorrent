package org.johnnei.javatorrent.bittorrent.tracker;

public enum TrackerEvent {

	EVENT_NONE(0),
	EVENT_COMPLETED(1),
	EVENT_STARTED(2),
	EVENT_STOPPED(3);

	private final int id;

	private TrackerEvent(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
