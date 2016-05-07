package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

/**
 * Tests {@link UtpPayloadFactory}
 */
public class UtpPayloadFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreateFromType() {
		UtpPayloadFactory cut = new UtpPayloadFactory();

		assertTrue("Expected data payload for id 0", cut.createPayloadFromType(0) instanceof DataPayload);
		assertTrue("Expected fin payload for id 1", cut.createPayloadFromType(1) instanceof FinPayload);
		assertTrue("Expected state payload for id 2", cut.createPayloadFromType(2) instanceof StatePayload);
		assertTrue("Expected reset payload for id 3", cut.createPayloadFromType(3) instanceof ResetPayload);
		assertTrue("Expected syn payload for id 4", cut.createPayloadFromType(4) instanceof SynPayload);
	}

	@Test
	public void testCreateFromTypeIncorrectType() {
		thrown.expect(IllegalArgumentException.class);

		new UtpPayloadFactory().createPayloadFromType(142412);
	}
}