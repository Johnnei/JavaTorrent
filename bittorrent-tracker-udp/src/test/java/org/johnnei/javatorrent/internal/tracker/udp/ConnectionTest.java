package org.johnnei.javatorrent.internal.tracker.udp;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionTest {

	@Test
	public void testIsValidFor() {
		// Create connection which is only valid for CONNECT
		Connection cut = new Connection(Clock.systemDefaultZone());

		for (TrackerAction action : TrackerAction.values()) {
			assertEquals(
				action == TrackerAction.CONNECT,
				cut.isValidFor(action, Clock.systemDefaultZone()),
				"Connection validity is incorrect on connect id."
			);
		}

		// Create connection which is valid for all but CONNECT
		cut = new Connection(5, Clock.systemDefaultZone());
		for (TrackerAction action : TrackerAction.values()) {
			assertEquals(
				cut.isValidFor(action, Clock.systemDefaultZone()),
				action != TrackerAction.CONNECT,
				"Connection validity is incorrect on connected id."
			);
		}

		// Create expired connection which is invalid for all packets
		cut = new Connection(5, Clock.systemDefaultZone());
		Clock offsetClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(5));
		for (TrackerAction action : TrackerAction.values()) {
			assertEquals(
				false,
				cut.isValidFor(action, offsetClock),
				"Connection validity is incorrect on expired id."
			);
		}
	}

	@Test
	public void testGetId() {
		Connection cut = new Connection(Clock.systemDefaultZone());
		assertEquals(0x41727101980L, cut.getId(), "Incorrect unconnected id");

		cut = new Connection(7, Clock.systemDefaultZone());
		assertEquals(7, cut.getId(), "Incorrect connected id");
	}

}
