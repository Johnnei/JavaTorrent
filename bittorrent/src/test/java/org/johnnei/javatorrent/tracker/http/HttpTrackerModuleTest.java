package org.johnnei.javatorrent.tracker.http;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.utils.CheckedBiFunction;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

		assertNotNull("Supplier is not allowed to return null values", supplier.getValue().apply("http://localhost.nl", mock(TorrentClient.class)));
	}

	@Test
	public void testGetRelatedBep() {
		assertEquals("HTTP Trackers are defined by BEP 3", 3, new HttpTrackerModule().getRelatedBep());
	}

	@Test
	public void testGetDependsOn() {
		assertEquals("No dependencies were expected", 0, new HttpTrackerModule().getDependsOn().size());
	}

	@Test
	public void testEmptyMethods() throws Exception {
		HttpTrackerModule cut = new HttpTrackerModule();
		cut.onBuild(null);
		cut.onPostHandshake(null);
		cut.onShutdown();
	}

}