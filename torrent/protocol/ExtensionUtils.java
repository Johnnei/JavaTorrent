package torrent.protocol;

import java.util.HashMap;

import torrent.protocol.messages.ut_metadata.UTMetadataExtension;

public class ExtensionUtils {

	private static ExtensionUtils instance = new ExtensionUtils();

	public static ExtensionUtils getUtils() {
		return instance;
	}

	private ExtensionUtils() {
		extensions = new HashMap<>();
		extensions.put(UTMetadata.ID, new UTMetadataExtension());
	}

	private HashMap<Integer, IExtension> extensions;

	public IExtension getExtension(int extensionId) {
		return extensions.get(extensionId);
	}

}
