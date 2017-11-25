package org.johnnei.javatorrent.bittorrent.tracker;

public enum TrackerEvent {

	EVENT_NONE(0, ""),
	EVENT_COMPLETED(1, "completed"),
	EVENT_STARTED(2, "started"),
	EVENT_STOPPED(3, "stopped");

	private final int id;

	private final String textual;

	private TrackerEvent(int id, String textual) {
		this.id = id;
		this.textual = textual;
	}

	/**
	 * @return The textual format as defined in BEP 3.
	 */
	public String getTextual() {
		return textual;
	}

	public int getId() {
		return id;
	}

}
