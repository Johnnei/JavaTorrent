package org.johnnei.javatorrent.internal;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClientSettings;

import static org.junit.jupiter.api.Assertions.*;

public class TorrentClientSettingsImplTest {

	@Test
	public void testPortBelowRange() {
		assertThrows(IllegalArgumentException.class,
			() -> new TorrentClientSettingsImpl.Builder().withAcceptingPort(0).build()
		);
	}

	@Test
	public void testPortAboveRange() {
		assertThrows(IllegalArgumentException.class,
			() -> new TorrentClientSettingsImpl.Builder().withAcceptingPort(65535 + 1).build()
		);
	}

	@Test
	public void testBuildDefaults() {
		TorrentClientSettings clientSettings = new TorrentClientSettingsImpl.Builder()
			.build();

		assertAll(
			() -> assertEquals(clientSettings.getAcceptingPort(), 6881),
			() -> assertFalse(clientSettings.isAcceptingConnections())
		);
	}

	@Test
	public void testBuild() {
		TorrentClientSettings clientSettings = new TorrentClientSettingsImpl.Builder()
			.withAcceptingPort(42)
			.withAcceptingConnections(true)
			.build();

		assertAll(
			() -> assertEquals(clientSettings.getAcceptingPort(), 42),
			() -> assertTrue(clientSettings.isAcceptingConnections())
		);
	}

}
