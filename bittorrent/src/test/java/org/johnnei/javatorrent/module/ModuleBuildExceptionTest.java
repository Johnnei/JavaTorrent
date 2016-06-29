package org.johnnei.javatorrent.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ModuleBuildException}
 */
public class ModuleBuildExceptionTest {

	@Test
	public void testConstructorCauseless() {
		ModuleBuildException e = new ModuleBuildException("test");

		assertEquals("Incorrect message", "test", e.getMessage());
	}

	@Test
	public void testConstructor() {
		IllegalArgumentException cause = new IllegalArgumentException("test cause");
		ModuleBuildException e = new ModuleBuildException("test", cause);

		assertEquals("Incorrect message", "test", e.getMessage());
		assertEquals("Incorrect cause", cause, e.getCause());
	}

}