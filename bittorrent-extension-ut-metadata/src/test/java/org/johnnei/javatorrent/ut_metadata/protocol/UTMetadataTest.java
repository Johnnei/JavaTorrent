package org.johnnei.javatorrent.ut_metadata.protocol;

import org.junit.Test;

import static org.johnnei.javatorrent.test.TestUtils.assertUtilityClassConstructor;
import static org.junit.Assert.assertEquals;

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
		assertEquals("Incorrect id for request message", 0, UTMetadata.REQUEST);
		assertEquals("Incorrect id for reject message", 2, UTMetadata.REJECT);
		assertEquals("Incorrect id for data message", 1, UTMetadata.DATA);
	}

}