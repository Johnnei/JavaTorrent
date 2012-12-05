package torrent.encoding;

import java.security.MessageDigest;

public class SHA1 {

	public static byte[] hash(String s) {
		return hash(s.getBytes());
	}

	public static byte[] hash(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return md.digest(data);
		} catch (Exception e) {
			return "SHA1_HASHING_NOT_FOUND".getBytes();
		}
	}

	public static boolean match(byte[] hash, byte[] hash1) {
		for (int i = 0; i < hash.length; i++) {
			if (hash[i] != hash1[i])
				return false;
		}
		return true;
	}

}
