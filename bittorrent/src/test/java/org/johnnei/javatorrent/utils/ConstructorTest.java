package org.johnnei.javatorrent.utils;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.test.TestUtils;

import org.junit.Test;

/**
 * Invokes the private constructors of the various utility classes to maximize the coverage validity.
 */
public class ConstructorTest {

	private static final Class<?>[] utilClasses = {
			MathUtils.class,
			StringUtils.class,
			BitTorrent.class,
			SHA1.class,
			Argument.class,
			Version.class
	};

	@Test
	public void testConstructors() throws Exception {
		for (Class<?> clazz : utilClasses) {
			TestUtils.assertUtilityClassConstructor(clazz);
		}
	}
}
