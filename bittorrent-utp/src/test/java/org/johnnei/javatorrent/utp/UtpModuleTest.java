package org.johnnei.javatorrent.utp;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.test.DummyEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpModule}
 */
public class UtpModuleTest {

	@Test
	public void testGetRelatedBep() {
		assertEquals("uTP should correctly report it's BEP.", 29, new UtpModule.Builder().build().getRelatedBep());
	}

	@Test
	public void testDependsOn() {
		assertEquals("uTP has no dependencies on other BEPs", 0, new UtpModule.Builder().build().getDependsOn().size());
	}

	@Test
	public void testConfigureTorrentClient() {
		// Nothing gets configured in the client, nothing should crash
		new UtpModule.Builder().build().configureTorrentClient(null);
	}

	@Test
	public void testOnPostHandshake() throws IOException {
		// Method is empty, nothing should crash.
		new UtpModule.Builder().build().onPostHandshake(null);
	}

	@Test
	public void testCreateSocketFactory() throws ModuleBuildException {
		UtpModule cut = new UtpModule.Builder().listenOn(DummyEntity.findAvailableUdpPort()).build();

		ScheduledExecutorService scheduleServiceMock = mock(ScheduledExecutorService.class);
		ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
		TorrentClient clientMock = mock(TorrentClient.class);
		when(clientMock.getExecutorService()).thenReturn(scheduleServiceMock);
		when(scheduleServiceMock.scheduleAtFixedRate(anyObject(), eq(0L), eq(100L), eq(TimeUnit.MILLISECONDS))).thenReturn(scheduledFuture);

		cut.onBuild(clientMock);

		try {
			assertNotNull("Factory should never produce null objects", cut.createSocketFactory().get());
		} finally {
			cut.onShutdown();
			verify(scheduledFuture).cancel(true);
		}
	}
}
