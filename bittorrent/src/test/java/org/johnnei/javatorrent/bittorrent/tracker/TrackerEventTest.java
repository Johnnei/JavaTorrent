package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link TrackerEvent}
 */
public class TrackerEventTest {

	@MethodSource("eventMapping")
	@DisplayName("Ensure Tracker Event IDs are correct.")
	@ParameterizedTest(name = "{0} => {1}")
	public void testEventIds(TrackerEvent event, int expectedId) {
		assertEquals(expectedId, event.getId());
	}

	public static Stream<Arguments> eventMapping() {
		return Stream.of(
			Arguments.of(TrackerEvent.EVENT_NONE, 0),
			Arguments.of(TrackerEvent.EVENT_COMPLETED, 1),
			Arguments.of(TrackerEvent.EVENT_STARTED, 2),
			Arguments.of(TrackerEvent.EVENT_STOPPED, 3)
		);
	}

}
