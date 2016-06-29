package org.johnnei.javatorrent;

public class Version {

	public static final String VERSION = "${project.version}";

	private static final String[] VERSION_PARTS = VERSION.split("[\\.\\-]");

	public static final String VERSION_MAJOR = VERSION_PARTS[0];

	public static final String VERSION_MINOR = VERSION_PARTS[1];

	public static final String VERSION_PATCH = VERSION_PARTS[2];

	public static final String BUILD = "JavaTorrent " + VERSION;

	private Version() {
		/* No instances for you! */
	}

}
