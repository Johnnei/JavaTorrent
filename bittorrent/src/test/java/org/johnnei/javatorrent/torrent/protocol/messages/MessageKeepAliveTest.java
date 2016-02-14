package org.johnnei.javatorrent.torrent.protocol.messages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertEquals("Incorrect length", 0, new MessageKeepAlive().getLength());

	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetId() throws Exception {
		new MessageKeepAlive().getId();
	}

	@Test
	public void testSetReadDuration() throws Exception {
		// No interaction is expected, pass null to make it throw NPE's if it still does
		new MessageKeepAlive().setReadDuration(null);
	}

	@Test
	public void testToString() throws Exception {
		assertTrue("toString doesn't contain class name", new MessageKeepAlive().toString().contains(MessageKeepAlive.class.getSimpleName()));
	}
}