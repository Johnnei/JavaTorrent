package org.johnnei.javatorrent.utp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.test.DummyEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
		assertEquals(29, new UtpModule.Builder().build().getRelatedBep(), "uTP should correctly report it's BEP.");
	}

	@Test
	public void testDependsOn() {
		assertEquals(0, new UtpModule.Builder().build().getDependsOn().size(), "uTP has no dependencies on other BEPs");
	}

	@Test
	public void testConfigureTorrentClient() {
		// Nothing gets configured in the client, nothing should crash
		new UtpModule.Builder().build().configureTorrentClient(null);
	}

	@Test
	public void testOnPostHandshake() {
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
		when(scheduleServiceMock.scheduleAtFixedRate(any(), eq(0L), eq(100L), eq(TimeUnit.MILLISECONDS))).thenReturn(scheduledFuture);
		when(scheduleServiceMock.scheduleWithFixedDelay(any(), eq(50L), eq(50L), eq(TimeUnit.MILLISECONDS))).thenReturn(scheduledFuture);

		cut.onBuild(clientMock);

		try {
			assertNotNull(cut.createSocketFactory().get(), "Factory should never produce null objects");
		} finally {
			cut.onShutdown();
			verify(scheduledFuture).cancel(true);
		}
	}
}
