package org.johnnei.javatorrent.torrent.download.peer;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Extensions {
	
	/**
	 * The map which maps the extension names to the ID
	 */
	private Map<String, Integer> extensionMap;
	
	/**
	 * The extension bytes as defined in the handshake
	 */
	private byte[] extensionBytes;
	
	public Extensions() {
		extensionMap = new HashMap<>();
	}
	
	/**
	 * Registers the extension
	 * @param name the name of the extension
	 * @param id the id of the extension
	 */
	public void register(String name, int id) {
		extensionMap.put(name, id);
	}
	
	/**
	 * Registers the extensions which are defined the handshake
	 * @param peerExtensionBytes the 8 bytes of the extension bytes. 
	 */
	public void register(byte[] peerExtensionBytes) {
		extensionBytes = peerExtensionBytes;
	}
	
	/**
	 * Checks if the {@link #extensionMap} has an entry for the given extension which is part of the Extended message standard
	 * @param extension the extension to test
	 * @return returns true if the extension is supported. Otherwise false
	 */
	public boolean hasExtension(String extension) {
		return extensionMap.containsKey(extension);
	}
	
	/**
	 * Checks if the {@link #extensionBytes} has the given bit set for the extension which is part of the extension bytes in the handshake
	 * @param index
	 * @param bit
	 * @return returns true if the extension bit is set. Otherwise false
	 */
	public boolean hasExtension(int index, int bit) {
		if (extensionBytes == null) {
			return false;
		}
		
		if (index < 0 || index >= extensionBytes.length) {
			return false;
		}
		
		return (extensionBytes[index] & bit) > 0;
	}
	
	/**
	 * Gets the id which is bound to the given extension
	 * @param extension the name of the extension
	 * @return the id of the extension
	 * @throws NoSuchElementException if the given extension is not contained in the {@link #extensionMap}
	 */
	public int getIdFor(String extension) throws NoSuchElementException {
		if (!hasExtension(extension)) {
			throw new NoSuchElementException(String.format("Extension is not registered: \"%s\"", extension));
		}
		
		return extensionMap.get(extension);
	}

}
