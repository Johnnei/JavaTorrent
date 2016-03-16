package org.johnnei.javatorrent.utils;

import java.lang.reflect.Constructor;

import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Invokes the private constructors of the various utility classes to maximize the coverage validity.
 */
public class ConstructorTest {

	private static final Class<?>[] utilClasses = {
			MathUtils.class,
			StringUtils.class,
			BitTorrent.class,
			SHA1.class
	};

	@Test
	public void testConstructors() throws Exception {
		for (Class<?> clazz : utilClasses) {
			Constructor<?>[] constructor = clazz.getDeclaredConstructors();
			assertEquals("Incorrect amount of constructors for util class", 1, constructor.length);
			assertFalse("Incorrect accessibility for constructor", constructor[0].isAccessible());
			constructor[0].setAccessible(true);
			constructor[0].newInstance();
		}
	}
}
