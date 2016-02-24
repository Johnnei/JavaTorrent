package org.johnnei.javatorrent.disk;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualsMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests {@link DiskJobWrapper}
 */
public class DiskJobWrapperTest extends EasyMockSupport {

	@Test
	public void testEquals() throws Exception {
		IDiskJob diskJobOne = createMock(IDiskJob.class);
		IDiskJob diskJobTwo = createMock(IDiskJob.class);

		DiskJobWrapper wrapperOne = new DiskJobWrapper(diskJobOne);
		DiskJobWrapper wrapperTwo = new DiskJobWrapper(diskJobOne);
		DiskJobWrapper wrapperThree = new DiskJobWrapper(diskJobTwo);

		assertEqualsMethod(wrapperOne);
		assertEquals("Equal wrappers don't match", wrapperOne, wrapperTwo);
		assertEquals("hashcode wrappers don't match", wrapperOne.hashCode(), wrapperTwo.hashCode());
		assertNotEquals("Non-Equal wrappers match", wrapperOne, wrapperThree);
	}
}