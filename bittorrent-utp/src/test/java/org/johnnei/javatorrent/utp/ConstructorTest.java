package org.johnnei.javatorrent.utp;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.test.TestUtils;


/**
 * Invokes the private constructors of the various utility classes to maximize the coverage validity.
 */
public class ConstructorTest {

	private static final Class<?>[] utilClasses = {
			Sync.class
	};

	@Test
	public void testConstructors() throws Exception {
		for (Class<?> clazz : utilClasses) {
			TestUtils.assertUtilityClassConstructor(clazz);
		}
	}
}
