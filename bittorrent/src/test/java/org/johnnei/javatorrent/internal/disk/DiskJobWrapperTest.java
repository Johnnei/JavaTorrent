package org.johnnei.javatorrent.internal.disk;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.test.TestUtils;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualsMethod;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link DiskJobWrapper}
 */
public class DiskJobWrapperTest {

	@Test
	public void testEquals() throws Exception {
		IDiskJob diskJobOne = mock(IDiskJob.class);
		IDiskJob diskJobTwo = mock(IDiskJob.class);

		DiskJobWrapper wrapperOne = new DiskJobWrapper(diskJobOne);
		DiskJobWrapper wrapperTwo = new DiskJobWrapper(diskJobOne);
		DiskJobWrapper wrapperThree = new DiskJobWrapper(diskJobTwo);

		assertEqualsMethod(wrapperOne);
		TestUtils.assertEqualityMethods(wrapperOne, wrapperTwo, wrapperThree);
	}
}
