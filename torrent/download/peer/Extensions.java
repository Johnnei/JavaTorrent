package torrent.download.peer;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Extensions {
	
	/**
	 * The map which maps the extension names to the ID
	 */
	private Map<String, Integer> extensionMap;
	
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
	 * Checks if the {@link #extensionMap} has an entry for the given extension
	 * @param extension the extension to test
	 * @return returns true if the extension is supported. Otherwise false
	 */
	public boolean hasExtension(String extension) {
		return extensionMap.containsKey(extension);
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
