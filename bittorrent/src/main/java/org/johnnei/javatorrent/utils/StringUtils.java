package org.johnnei.javatorrent.utils;


public class StringUtils {

	public static String byteArrayToString(byte[] array) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : array) {
			String hex = Integer.toHexString(b & 0xFF).toUpperCase();
			if (hex.length() == 1) {
				stringBuilder.append("0");
			}
			stringBuilder.append(hex);
		}
		return stringBuilder.toString();
	}

}
