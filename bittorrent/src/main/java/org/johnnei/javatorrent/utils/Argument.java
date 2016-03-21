package org.johnnei.javatorrent.utils;

/**
 * A class which mimics {@link java.util.Objects} but throws {@link IllegalArgumentException} instead of {@link NullPointerException}
 */
public class Argument {

	private Argument() {
		/* No utils class for you! */
	}

	/**
	 * Tests for null and throws {@link IllegalArgumentException} when null.
	 * @param object The object to test.
	 * @param message The message when it is null.
	 * @param <T> The type
	 * @return The object instance when non-null.
	 */
	public static <T> T requireNonNull(T object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}

		return object;
	}

	/**
	 * Tests if the given integer is positive.
	 * @param amount The amount to test.
	 * @param message The message when the amount is negative.
	 */
	public static void requirePositive(int amount, String message) {
		if (amount >= 0) {
			return;
		}

		throw new IllegalArgumentException(message);
	}

	/**
	 * Tests if the given amount is within the bounds.
	 * @param amount The amount to test
	 * @param lowerLimit The lower limit (inclusive)
	 * @param upperLimit The upper limit (exclusive)
	 */
	public static void requireWithinBounds(int amount, int lowerLimit, int upperLimit, String message) {
		if (amount >= lowerLimit && amount < upperLimit) {
			return;
		}

		throw new IllegalArgumentException(message);
	}
}
