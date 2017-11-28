package org.johnnei.javatorrent.tracker.http;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.utils.CheckedBiFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link HttpTrackerModule}
 */
public class HttpTrackerModuleTest {

	@Test
	public void testConfigureTorrentClient() throws Exception {
		TorrentClient.Builder torrentClientBuilderMock = mock(TorrentClient.Builder.class);

		HttpTrackerModule cut = new HttpTrackerModule();
		cut.configureTorrentClient(torrentClientBuilderMock);

		ArgumentCaptor<CheckedBiFunction> supplier = ArgumentCaptor.forClass(CheckedBiFunction.class);

		verify(torrentClientBuilderMock).registerTrackerProtocol(eq("http"), supplier.capture());

		assertNotNull(supplier.getValue().apply("http://localhost.nl", mock(TorrentClient.class)), "Supplier is not allowed to return null values");
	}

	@Test
	public void testGetRelatedBep() {
		assertEquals(3, new HttpTrackerModule().getRelatedBep(), "HTTP Trackers are defined by BEP 3");
	}

	@Test
	public void testGetDependsOn() {
		assertThat("No dependencies were expected", new HttpTrackerModule().getDependsOn(), empty());
	}

	@Test
	public void testEmptyMethods() throws Exception {
		HttpTrackerModule cut = new HttpTrackerModule();
		cut.onBuild(null);
		cut.onPostHandshake(null);
		cut.onShutdown();
	}

}
