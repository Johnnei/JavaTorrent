package org.johnnei.javatorrent.bittorrent.encoding;

import java.security.MessageDigest;

public class SHA1 {

	/**
	 * Hashes the given data into SHA-1 Hashing
	 *
	 * @param data The bytes to hash
	 * @return The 20-byte hash
	 */
	public static byte[] hash(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return md.digest(data);
		} catch (Exception e) {
			return new byte[20];
		}
	}

}
