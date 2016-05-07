package org.johnnei.javatorrent.utp;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;
import org.johnnei.javatorrent.test.TestUtils;

import org.junit.Test;

/**
 * Invokes the private constructors of the various utility classes to maximize the coverage validity.
 */
public class ConstructorTest {

	private static final Class<?>[] utilClasses = {
			UtpProtocol.class,
	};

	@Test
	public void testConstructors() throws Exception {
		for (Class<?> clazz : utilClasses) {
			TestUtils.assertUtilityClassConstructor(clazz);
		}
	}
}
