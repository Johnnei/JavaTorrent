package org.johnnei.javatorrent.utils;

/**
 * A class which mimics {@link java.util.Objects} but throws {@link IllegalArgumentException} instead of {@link NullPointerException}
 */
public class Argument {

	private Argument() {
		/* No utils class for you! */
	}

	public static <T> T requireNonNull(T object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}

		return object;
	}
}
