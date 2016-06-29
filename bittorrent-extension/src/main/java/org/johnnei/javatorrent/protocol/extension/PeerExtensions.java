package org.johnnei.javatorrent.protocol.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Peer module info to store which extensions (BEP #10) are supported by this peer
 *
 * @since 0.5
 */
public class PeerExtensions {

	private Map<String, Integer> extensionMap;

	public PeerExtensions() {
		extensionMap = new HashMap<>();
	}

	/**
	 * Tests if the peer supports the given extension
	 * @param name The name of the extension
	 * @return <code>true</code> if supported, otherwise <code>false</code>
	 * @since 0.5
	 *
	 * @see #getExtensionId(String)
	 */
	public boolean hasExtension(String name) {
		return extensionMap.containsKey(name);
	}

	/**
	 * Gets the ID which the peer expects in packets for the given extension
	 * @param name The name of the extension
	 * @return the ID as reported by the peer of the extension.
	 * @throws NoSuchElementException if the peer doesn't support the given extension
	 * @since 0.5
	 *
	 * @see #hasExtension(String)
	 */
	public int getExtensionId(String name) {
		if (!hasExtension(name)) {
			throw new NoSuchElementException(String.format("Peer doesn't support %s", name));
		}

		return extensionMap.get(name);
	}

	/**
	 * Registers the ID as reported by the Peer to the given extension name
	 * @param id The ID for the extension
	 * @param name The name of the extension
	 * @throws IllegalStateException When duplicates are inserted.
	 * @since 0.5
	 */
	public void registerExtension(int id, String name) {
		if (extensionMap.containsKey(name)) {
			throw new IllegalStateException(String.format("Inserting second ID for %s.", name));
		}

		extensionMap.put(name, id);
	}
}
