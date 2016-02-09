package org.johnnei.javatorrent.protocol.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

public class PeerExtensionsTest {

	@Test(expected=NoSuchElementException.class)
	public void testGetExtensionIdMissing() {
		PeerExtensions cut = new PeerExtensions();
		cut.getExtensionId("jt_stub");
	}

	@Test
	public void testGetExtensionId() {
		PeerExtensions cut = new PeerExtensions();

		assertFalse("Got extension before registered.", cut.hasExtension("jt_stub"));

		cut.registerExtension(1, "jt_stub");

		assertTrue("Doesn't got extension after registered.", cut.hasExtension("jt_stub"));
		assertEquals("Incorrect ID returned", 1, cut.getExtensionId("jt_stub"));

	}

	@Test(expected=IllegalStateException.class)
	public void testRegisterExtensionDuplicate() {
		PeerExtensions cut = new PeerExtensions();
		cut.registerExtension(1, "jt_stub");
		cut.registerExtension(1, "jt_stub");
	}

}
