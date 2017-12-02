package org.johnnei.javatorrent.ut.metadata.protocol;

import org.junit.jupiter.api.Test;

import static org.johnnei.javatorrent.test.TestUtils.assertUtilityClassConstructor;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link UTMetadata}
 */
public class UTMetadataTest {

	@Test
	public void testConstructor() throws Exception {
		assertUtilityClassConstructor(UTMetadata.class);
	}

	@Test
	public void testConstants() {
		assertEquals(0, UTMetadata.REQUEST, "Incorrect id for request message");
		assertEquals(2, UTMetadata.REJECT, "Incorrect id for reject message");
		assertEquals(1, UTMetadata.DATA, "Incorrect id for data message");
	}

}
