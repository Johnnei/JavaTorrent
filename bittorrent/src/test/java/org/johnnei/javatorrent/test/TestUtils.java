package org.johnnei.javatorrent.test;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {

	private TestUtils() {
		/* No constructors for you! */
	}

	public static void copySection(byte[] from, byte[] to, int targetStart) {
		copySection(from, to, targetStart, 0, from.length);
	}

	public static void copySection(byte[] from, byte[] to, int targetStart, int start, int count) {
		for (int i = 0; i < count; i++) {
			to[targetStart + i] = from[start + i];
		}
	}

	/**
	 * Tests the 3 mandatory checks on the equals method:
	 * <ol>
	 *   <li>this == this -&gt; true</li>
	 *   <li>this == null -&gt; false</li>
	 *   <li>this not instance of type -&gt; false (Tested with comparing the integer 7)</li>
	 * </ul>
	 * @param o The object to test on
	 */
	public static void assertEqualsMethod(Object o) {
		assertEquals(o, o, "Object must equal itself.");
		assertFalse(o.equals(null), "Object must never match null");
		assertFalse(o.equals(7), "Object matches with not castable type");
	}

	public static void assertEqualityMethods(Object base, Object equalToBase, Object... notEqualToBase) {
		assertEqualsMethod(base);
		assertTrue(base.equals(equalToBase), "Base didn't equal with the given equal");
		assertEquals(base.hashCode(), equalToBase.hashCode(), "Base hashcode didn't match with given equal");

		for (Object notEqual : notEqualToBase) {
			assertThat("Base matched with the given non-equal", base, not(equalTo(notEqual)));
		}
	}

	public static void assertNotPresent(String message, Optional<?> optional) {
		assertFalse(optional.isPresent(), message);
	}

	public static <T> T assertPresent(String message, Optional<T> optional) {
		return optional.orElseThrow(() -> new AssertionError(message));
	}

	public static void assertUtilityClassConstructor(Class<?> clazz) throws Exception {
		Constructor<?>[] constructor = clazz.getDeclaredConstructors();
		assertEquals(1, constructor.length, "Incorrect amount of constructors for util class");
		assertFalse(constructor[0].isAccessible(), "Incorrect accessibility for constructor");
		constructor[0].setAccessible(true);
		constructor[0].newInstance();
	}

}
