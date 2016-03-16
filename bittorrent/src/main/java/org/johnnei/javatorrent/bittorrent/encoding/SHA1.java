package org.johnnei.javatorrent.bittorrent.encoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {

	private SHA1() {
		/* No SHA1 instances for you */
	}

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
		} catch (NoSuchAlgorithmException e) {
			// SHA-1 is mandatory by the Java spec so this should never be thrown.
			throw new UnsupportedOperationException("SHA-1 implementation is missing. Can't verify downloads", e);
		}
	}

}
