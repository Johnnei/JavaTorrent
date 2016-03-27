package org.johnnei.javatorrent.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by johnn on 26/02/2016.
 */
public class MathUtilsTest {

	@Test
	public void testCeilDivision() throws Exception {
		int divisor = 7;

		assertEquals("Clean division resulted in ceiling", 0, MathUtils.ceilDivision(0, divisor));
		for (int i = 1; i <= divisor; i++) {
			assertEquals("Clean division resulted in ceiling", 1, MathUtils.ceilDivision(i, divisor));
		}
		assertEquals("n+1 division resulted in wrong result", 2, MathUtils.ceilDivision(8, divisor));
	}

	@Test
	public void testCeilDivisionLong() throws Exception {
		int divisor = 7;

		assertEquals("Clean division resulted in ceiling", 0, MathUtils.ceilDivision(0, divisor));
		for (long i = 1; i <= divisor; i++) {
			assertEquals("Clean division resulted in ceiling", 1, MathUtils.ceilDivision(i, divisor));
		}
		assertEquals("n+1 division resulted in wrong result", 2, MathUtils.ceilDivision(8, divisor));

	}
}