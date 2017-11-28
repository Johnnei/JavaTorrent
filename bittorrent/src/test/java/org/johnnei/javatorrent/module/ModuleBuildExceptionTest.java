package org.johnnei.javatorrent.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ModuleBuildException}
 */
public class ModuleBuildExceptionTest {

	@Test
	public void testConstructorCauseless() {
		ModuleBuildException e = new ModuleBuildException("test");

		assertEquals("test", e.getMessage(), "Incorrect message");
	}

	@Test
	public void testConstructor() {
		IllegalArgumentException cause = new IllegalArgumentException("test cause");
		ModuleBuildException e = new ModuleBuildException("test", cause);

		assertEquals("test", e.getMessage(), "Incorrect message");
		assertEquals(cause, e.getCause(), "Incorrect cause");
	}

}
