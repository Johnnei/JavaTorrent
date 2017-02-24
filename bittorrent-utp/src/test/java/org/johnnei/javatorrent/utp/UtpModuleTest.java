package org.johnnei.javatorrent.utp;

import java.io.IOException;

import org.johnnei.javatorrent.TorrentClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link UtpModule}
 */
public class UtpModuleTest {

	@Test
	public void testGetRelatedBep() {
		assertEquals(29, new UtpModule().getRelatedBep());
	}

	@Test
	public void testDependsOn() {
		assertEquals(0, new UtpModule().getDependsOn().size());
	}

	@Test
	public void testConfigureTorrentClient() {
		// Nothing gets configured in the client, nothing should crash
		new UtpModule().configureTorrentClient(null);
	}

	@Test
	public void testOnPostHandshake() throws IOException {
		// Method is empty, nothing should crash.
		new UtpModule().onPostHandshake(null);
	}

	@Test
	public void testCreateSocketFactory() {
		UtpModule cut = new UtpModule();
		assertNotNull("Factory should never produce null objects", cut.createSocketFactory().get());
	}

	@Test
	public void testOnBuildAndShutdown() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		UtpModule cut = new UtpModule();
		cut.onBuild(torrentClientMock);
		cut.onShutdown();
	}
}