package torrent.util;

public class StringUtil {

	public static String speedToString(double speed) {
		String[] names = { " b/s", " Kb/s", " Mb/s" };
		int pointer = 0;
		while (pointer < names.length && speed >= 1000) {
			speed /= 1000;
			++pointer;
		}
		String stringSpeed = Double.toString(speed);
		if (stringSpeed.contains(".")) {
			String[] parts = stringSpeed.split("\\.");
			if (parts[1].length() == 1 && parts[1].equals("0")) {
				stringSpeed = parts[0];
			} else {
				stringSpeed = parts[0] + "." + parts[1].substring(0, 1);
			}
		}
		return stringSpeed + names[pointer];
	}

	/**
	 * Translates the hexadecimal encoding back to readable text
	 * 
	 * @param s
	 * @return
	 */
	public static String removeHex(String s) {
		String[] pieces = s.split("%");
		for (int i = 1; i < pieces.length; i++) {
			int hex = Integer.parseInt(pieces[i].substring(0, 2), 16);
			pieces[i] = (char) hex + pieces[i].substring(2);
		}
		s = pieces[0];
		for (int i = 1; i < pieces.length; i++)
			s += pieces[i];
		return s;
	}

	public static String decodeHex(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i += 2) {
			result += (char) Integer.parseInt(s.substring(i, i + 2), 16);
		}
		return result;
	}

	public static String byteArrayToString(byte[] array) {
		String s = "";
		for (byte b : array) {
			String hex = Integer.toHexString(b & 0xFF).toUpperCase();
			if(hex.length() == 1)
				hex = "0" + hex;
			s += hex;
		}
		return s;
	}

	/**
	 * Replaces all occurrences of "+" with " "
	 * 
	 * @param s
	 * @return
	 */
	public static String spaceFix(String s) {
		return s.replaceAll("\\+", " ");
	}

	public static String peerIdToString(byte[] peerId) {
		String s = byteArrayToString(peerId);
		return decodeHex(s.substring(0, 16)) + s.substring(16);
	}

}
