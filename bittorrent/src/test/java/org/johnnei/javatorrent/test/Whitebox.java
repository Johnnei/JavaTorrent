package org.johnnei.javatorrent.test;

import java.lang.reflect.Field;

public class Whitebox {

	public static void setInternalState(Object this_, String fieldName, Object value) {
		try {
			Field field = this_.getClass().getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}

			field.set(this_, value);
		} catch (Exception e) {
			throw new AssertionError(String.format("Failed to inject value into %s#%s", this_.getClass().getName(), fieldName));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object this_, String fieldName) {
		try {
			Field field = this_.getClass().getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}

			return (T) field.get(this_);
		} catch (Exception e) {
			throw new AssertionError(String.format("Failed to inject value into %s#%s", this_.getClass().getName(), fieldName));
		}
	}

}
