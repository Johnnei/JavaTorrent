package org.johnnei.javatorrent.ut_metadata.protocol.messages;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link MessageUnknown}
 */
public class MessageUnknownTest {

	private final static String ERR_MESSAGE = "This message can not be written, we don't know what it is!";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testWrite() throws Exception {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage(ERR_MESSAGE);

		MessageUnknown cut = new MessageUnknown();
		cut.write(null);
	}

	@Test
	public void testRead() throws Exception {
		MessageUnknown cut = new MessageUnknown();

		// No interaction expected, so it shouldn't break with null
		cut.read(null);
	}

	@Test
	public void testProcess() throws Exception {
		MessageUnknown cut = new MessageUnknown();

		// No interaction expected, so it shouldn't break with null
		cut.process(null);
	}

	@Test
	public void testGetLength() throws Exception {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage(ERR_MESSAGE);

		MessageUnknown cut = new MessageUnknown();
		cut.getLength();
	}

	@Test
	public void testGetId() throws Exception {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage(ERR_MESSAGE);

		MessageUnknown cut = new MessageUnknown();
		cut.getId();
	}

}