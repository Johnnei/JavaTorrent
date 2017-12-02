package org.johnnei.javatorrent.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by johnn on 26/02/2016.
 */
public class MathUtilsTest {

	@Test
	public void testCeilDivision() throws Exception {
		int divisor = 7;

		assertEquals(0, MathUtils.ceilDivision(0, divisor), "Clean division resulted in ceiling");
		for (int i = 1; i <= divisor; i++) {
			assertEquals(1, MathUtils.ceilDivision(i, divisor), "Clean division resulted in ceiling");
		}
		assertEquals(2, MathUtils.ceilDivision(8, divisor), "n+1 division resulted in wrong result");
	}

	@Test
	public void testCeilDivisionLong() throws Exception {
		int divisor = 7;

		assertEquals(0, MathUtils.ceilDivision(0, divisor), "Clean division resulted in ceiling");
		for (long i = 1; i <= divisor; i++) {
			assertEquals(1, MathUtils.ceilDivision(i, divisor), "Clean division resulted in ceiling");
		}
		assertEquals(2, MathUtils.ceilDivision(8, divisor), "n+1 division resulted in wrong result");

	}
}
