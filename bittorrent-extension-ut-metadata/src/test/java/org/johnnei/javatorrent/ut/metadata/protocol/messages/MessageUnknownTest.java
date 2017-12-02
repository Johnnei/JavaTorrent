package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MessageUnknown}
 */
public class MessageUnknownTest {

	private final static String ERR_MESSAGE = "This message can not be written, we don't know what it is!";

	@Test
	public void testWrite() throws Exception {
		MessageUnknown cut = new MessageUnknown();
		Exception e = assertThrows(UnsupportedOperationException.class, () -> cut.write(null));
		assertThat(e.getMessage(), containsString(ERR_MESSAGE));
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
		MessageUnknown cut = new MessageUnknown();
		Exception e = assertThrows(UnsupportedOperationException.class, cut::getLength);
		assertThat(e.getMessage(), containsString(ERR_MESSAGE));
	}

	@Test
	public void testGetId() throws Exception {
		MessageUnknown cut = new MessageUnknown();
		Exception e = assertThrows(UnsupportedOperationException.class, cut::getId);
		assertThat(e.getMessage(), containsString(ERR_MESSAGE));
	}

}
