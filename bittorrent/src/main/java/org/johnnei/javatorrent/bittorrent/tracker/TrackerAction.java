package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.stream.Stream;

public enum TrackerAction {

	CONNECT(0),
	ANNOUNCE(1),
	SCRAPE(2),
	ERROR(3);

	private final int id;

	private TrackerAction(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static final TrackerAction of(int id) {
		return Stream.of(values())
				.filter(action -> id == action.getId())
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Unknown tracker action with ID: %d", id)));
	}

}
