package org.johnnei.javatorrent.bittorrent.encoding;

@Deprecated
public class Bencoder {

	private String bencoded;

	public Bencoder() {
		bencoded = "";
	}

	public void integer(int i) {
		bencoded += "i" + Integer.toString(i) + "e";
	}

	public void integer(long l) {
		bencoded += "i" + Long.toString(l) + "e";
	}

	public void string(String s) {
		bencoded += s.length() + ":" + s;
	}

	public void listStart() {
		bencoded += "l";
	}

	public void listEnd() {
		bencoded += "e";
	}

	public void dictionaryStart() {
		bencoded += "d";
	}

	public void dictionaryEnd() {
		bencoded += "e";
	}

	public String getBencodedData() {
		return bencoded;
	}

}
