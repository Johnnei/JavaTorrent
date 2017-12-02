package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the KeepAlive contract
 */
public class MessageKeepAliveTest {

	@Test
	public void testWrite() throws Exception {
		// No interaction is expected, pass null to make it throw NPE's if it still does
		new MessageKeepAlive().write(null);
	}

	@Test
	public void testRead() throws Exception {
		// No interaction is expected, pass null to make it throw NPE's if it still does
		new MessageKeepAlive().read(null);
	}

	@Test
	public void testProcess() throws Exception {
		// No interaction is expected, pass null to make it throw NPE's if it still does
		new MessageKeepAlive().process(null);
	}

	@Test
	public void testGetLength() throws Exception {
		assertEquals(0, new MessageKeepAlive().getLength(), "Incorrect length");
	}

	@Test
	public void testGetId() throws Exception {
		assertThrows(UnsupportedOperationException.class, () -> new MessageKeepAlive().getId());
	}

	@Test
	public void testToString() throws Exception {
		assertTrue(new MessageKeepAlive().toString().contains(MessageKeepAlive.class.getSimpleName()), "toString doesn't contain class name");
	}
}
