package org.johnnei.javatorrent.torrent.util;


public class StringUtil {

	/**
	 * Compacts a size of a file/speed in bytes to their smaller notations (kb, mb, etc) upto (including) TB
	 * 
	 * @param size The size in bytes
	 * @return The string which equals the size in the smallest notation
	 */
	public static String compactByteSize(double size) {
		String[] names = { "B", "KB", "MB", "GB", "TB" };
		int pointer = 0;
		while (pointer < names.length && size >= 1000) {
			size /= 1000;
			++pointer;
		}
		String stringSpeed = Double.toString(size);
		if (stringSpeed.contains(".")) {
			String[] parts = stringSpeed.split("\\.");
			if (parts[1].length() == 1 && parts[1].equals("0")) {
				stringSpeed = parts[0];
			} else {
				stringSpeed = parts[0] + "." + parts[1].substring(0, 1);
			}
		}
		return stringSpeed + " " + names[pointer];
	}

	/**
	 * Converts seconds into the HH:MM:SS string
	 * @param seconds The amount of seconds
	 * @return The string which formats the time
	 */
	public static String timeToString(long seconds) {
		if (seconds < 10) {
			return "0:0" + seconds;
		} else if (seconds < 60) {
			return "0:" + seconds;
		} else {
			int hours = 0;
			int minutes = 0;
			while (seconds >= 3600) {
				++hours;
				seconds -= 3600;
			}
			while (seconds >= 60) {
				++minutes;
				seconds -= 60;
			}
			String sHours = Integer.toString(hours);
			String sMinutes = Integer.toString(minutes);
			String sSeconds = Long.toString(seconds);
			if (sSeconds.length() < 2)
				sSeconds = "0" + sSeconds;
			if (hours > 0) {
				if (sMinutes.length() < 2)
					sMinutes = "0" + sMinutes;
				return sHours + ":" + sMinutes + ":" + sSeconds;
			} else {
				return sMinutes + ":" + sSeconds;
			}
		}
	}

	public static String progressToString(double d) {
		String progressString = Double.toString(d);
		if (progressString.equals("NaN"))
			progressString = "0.0";
		if (progressString.contains(".")) {
			int pointIndex = progressString.indexOf(".");
			progressString = progressString.substring(0, Math.min(pointIndex + 3, progressString.length()));
		}
		return progressString;
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
			if (hex.length() == 1)
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
