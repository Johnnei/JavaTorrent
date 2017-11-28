package org.johnnei.javatorrent.protocol.extension;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PeerExtensionsTest {

	@Test
	public void testGetExtensionIdMissing() {
		PeerExtensions cut = new PeerExtensions();
		assertThrows(NoSuchElementException.class, () -> cut.getExtensionId("jt_stub"));
	}

	@Test
	public void testGetExtensionId() {
		PeerExtensions cut = new PeerExtensions();

		assertFalse(cut.hasExtension("jt_stub"), "Got extension before registered.");

		cut.registerExtension(1, "jt_stub");

		assertTrue(cut.hasExtension("jt_stub"), "Doesn't got extension after registered.");
		assertEquals(1, cut.getExtensionId("jt_stub"), "Incorrect ID returned");

	}

	@Test
	public void testRegisterExtensionDuplicate() {
		PeerExtensions cut = new PeerExtensions();
		cut.registerExtension(1, "jt_stub");
		assertThrows(IllegalStateException.class, () -> cut.registerExtension(1, "jt_stub"));
	}

}
