package org.johnnei.javatorrent.utils.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.Properties;

import org.johnnei.javatorrent.utils.ThreadUtils;

public class Config {

	/**
	 * The singleton config instance
	 */
	private static Config instance;

	/**
	 * Gets the singleton config file for JavaTorrent
	 *
	 * @return The configuration
	 */
	public static Config getConfig() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	/**
	 * The properties of the client
	 */
	private Properties properties;

	/**
	 * The file which the config is being stored
	 */
	private File configFile;

	/**
	 * The application data folder in which we store config/temp data
	 */
	private String folder;

	private Config() {
		Properties defaultProperties = new Properties();
		defaultProperties.put("peer-max", 500);
		defaultProperties.put("peer-max_burst_ratio", 1.5F);
		defaultProperties.put("peer-max_concurrent_connecting", 2);
		defaultProperties.put("peer-max_connecting", 50);
		defaultProperties.put("download-output_folder", ".\\");
		defaultProperties.put("download-port", 6881);
		defaultProperties.put("general-show_all_peers", false);

		properties = new Properties(defaultProperties);

		getFile("JavaTorrent.cfg");
	}

	private void getFile(String filename) {
		folder = System.getProperty("user.home") + "\\";
		String os = System.getProperty("os.name");
		if (os.equals("Windows 7") || os.equals("Windows Vista")) {
			folder += "AppData\\Roaming\\JavaTorrent\\";
			new File(folder).mkdirs();
		}
		configFile = new File(folder + filename);
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads all settings from the config file
	 */
	public void load() {
		try (BufferedReader inputStream = new BufferedReader(new FileReader(configFile))){
			properties.load(inputStream);
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			load();
		}
	}

	/**
	 * Saves the config file to the hdd
	 */
	public void save() {
		try (BufferedWriter outStream = new BufferedWriter(new FileWriter(configFile))) {
			properties.store(outStream, "JavaTorrent GUI Configuration");
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			save();
		}
	}

	/**
	 * Gets a value from the config
	 *
	 * @param key The key of the pair
	 * @return The value by the given key
	 */
	private String get(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException(getMissingConfigError(key));
		} else {
			return value;
		}
	}

	/**
	 * Overrides a pair of values
	 *
	 * @param key The key of the pair
	 * @param value The value of the pair
	 */
	public void set(String key, Object value) {
		properties.put(key, value.toString());
		save();
	}

	/**
	 * Adds a pair of values if it doesn't exists
	 * @param key The key of the pair
	 * @param value The value of the pair
	 */
	public void setDefault(String key, Object value) {
		if(!properties.containsKey(key)) {
			set(key, value);
		}
	}

	/**
	 * Gets an integer config value
	 *
	 * @param key The key of the pair
	 * @return The value by the given key
	 */
	public int getInt(String key) {
		String val = get(key);
		if (isInt(val)) {
			return Integer.parseInt(val);
		} else {
			throw new IllegalArgumentException(getMissingConfigError(key));
		}
	}

	public boolean getBoolean(String key) {
		String val = get(key);
		if (isBoolean(val)) {
			return parseBoolean(val);
		} else {
			throw new IllegalArgumentException(getMissingConfigError(key));
		}
	}

	/**
	 * Gets an float config value
	 *
	 * @param key The key of the pair
	 * @return The value by the given key
	 */
	public float getFloat(String key) {
		String val = get(key);
		if (isFloat(val)) {
			return Float.parseFloat(val);
		} else {
			throw new IllegalArgumentException(getMissingConfigError(key));
		}
	}

	/**
	 * Gets an String config value
	 *
	 * @param key The key of the pair
	 * @return The value by the given key
	 */
	public String getString(String key) {
		return get(key);
	}

	public String getTempFolder() {
		return folder;
	}

	public File getTorrentFileFor(String torrentHash) {
		return new File(String.format("%s%s.torrent", getTempFolder(), torrentHash));
	}

	private boolean isBoolean(String s) {
		try {
			parseBoolean(s);
			return true;
		} catch (IllegalFormatException e) {
			return false;
		}
	}

	private boolean parseBoolean(String s) {
		s = s.toLowerCase();
		String[] trueList = new String[] { "yes", "1", "true" };
		String[] falseList = new String[] { "no", "0", "false" };
		for(int i = 0; i < trueList.length; i++) {
			if(trueList[i].equals(s)) {
				return true;
			} else if(falseList[i].equals(s)) {
				return false;
			}
		}
		throw new NumberFormatException(String.format("Invalid boolean string: %s", s));
	}

	public static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isFloat(String s) {
		try {
			Float.parseFloat(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String getMissingConfigError(String key) {
		return String.format("Configuration Setting \"%s\" has not been registered", key);
	}

}
