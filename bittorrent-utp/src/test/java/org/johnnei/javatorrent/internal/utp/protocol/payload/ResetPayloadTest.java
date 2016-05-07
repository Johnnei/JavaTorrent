package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ResetPayload}
 */
public class ResetPayloadTest {

	@Test
	public void testGetType() {
		assertEquals(3, new ResetPayload().getType());
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new ResetPayload().toString().startsWith("ResetPayload["));
	}

}