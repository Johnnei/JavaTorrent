package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TrackerAction}
 */
public class TrackerActionTest {

	@ParameterizedTest
	@MethodSource("actionIdMapping")
	public void testConnect(TrackerAction action, int expectedId) {
		assertEquals(expectedId, action.getId());
	}

	public static Stream<Arguments> actionIdMapping() {
		return Stream.of(
			Arguments.of(TrackerAction.CONNECT, 0),
			Arguments.of(TrackerAction.ANNOUNCE, 1),
			Arguments.of(TrackerAction.SCRAPE, 2),
			Arguments.of(TrackerAction.ERROR, 3)
		);
	}

	@Test
	public void testIncorrectId() {
		assertThrows(IllegalArgumentException.class, () -> TrackerAction.of(4));
	}
}
