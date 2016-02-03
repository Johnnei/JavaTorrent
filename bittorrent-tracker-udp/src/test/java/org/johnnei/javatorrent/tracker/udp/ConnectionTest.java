package org.johnnei.javatorrent.tracker.udp;

import java.time.Clock;
import java.time.Duration;

import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionTest {

	@Test
	public void testIsValidFor() {
		// Create connection which is only valid for CONNECT
		Connection cut = new Connection(Clock.systemDefaultZone());

		for (TrackerAction action : TrackerAction.values()) {
			Assert.assertEquals(
					"Connection validity is incorrect on connect id.",
					action == TrackerAction.CONNECT,
					cut.isValidFor(action, Clock.systemDefaultZone()));
		}

		// Create connection which is valid for all but CONNECT
		cut = new Connection(5, Clock.systemDefaultZone());
		for (TrackerAction action : TrackerAction.values()) {
			Assert.assertEquals(
					"Connection validity is incorrect on connected id.",
					action != TrackerAction.CONNECT,
					cut.isValidFor(action, Clock.systemDefaultZone()));
		}

		// Create expired connection which is invalid for all packets
		cut = new Connection(5, Clock.systemDefaultZone());
		Clock offsetClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(5));
		for (TrackerAction action : TrackerAction.values()) {
			Assert.assertEquals(
					"Connection validity is incorrect on expired id.",
					false,
					cut.isValidFor(action, offsetClock));
		}
	}

	@Test
	public void testGetId() {
		Connection cut = new Connection(Clock.systemDefaultZone());
		Assert.assertEquals("Incorrect unconnected id", 0x41727101980L, cut.getId());

		cut = new Connection(7, Clock.systemDefaultZone());
		Assert.assertEquals("Incorrect connected id", 7, cut.getId());
	}

}
