package org.johnnei.utils.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map.Entry;

import org.johnnei.utils.ThreadUtils;

import torrent.download.Torrent;

//TODO Check Linux Support, Other OS might not work with OS home

public class Config {

	/**
	 * The singleton config instance
	 */
	private static Config instance;

	/**
	 * Gets the singleton config file for JavaTorrent
	 * 
	 * @return
	 */
	public static Config getConfig() {
		if (instance == null)
			instance = new Config();
		return instance;
	}

	/**
	 * The file which the config is being stored
	 */
	private File configFile;
	/**
	 * The application data folder in which we store config/temp data
	 */
	private String folder;
	/**
	 * All configs which are located in the config file
	 */
	private HashMap<String, String> config;

	private Config() {
		getFile("JavaTorrent.cfg");
		config = new HashMap<>();
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
		BufferedReader inputStream = null;
		try {
			inputStream = new BufferedReader(new FileReader(configFile));
			String line = null;
			while ((line = inputStream.readLine()) != null) {
				if (line.trim().length() > 0 && line.contains("=")) {
					String[] data = line.split("=");
					String key = data[0];
					String value = line.substring(key.length() + 1);
					config.put(key, value);
				}
			}
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			load();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Saves the config file to the hdd
	 */
	public void save() {
		BufferedWriter outStream = null;
		try {
			outStream = new BufferedWriter(new FileWriter(configFile));
			for (Entry<String, String> entry : config.entrySet()) {
				outStream.write(String.format("%s=%s\n", entry.getKey(), entry.getValue()));
			}
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			save();
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Gets a value from the config
	 * 
	 * @param key The key of the pair
	 * @param defaultValue The default value for the pair if it doesn't exist
	 * @return The value by the given key
	 */
	private String get(String key) {
		String value = config.get(key);
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
		config.put(key, value.toString());
		save();
	}
	
	/**
	 * Adds a pair of values if it doesn't exists
	 * @param key The key of the pair
	 * @param value The value of the pair
	 */
	public void setDefault(String key, Object value) {
		if(!config.containsKey(key)) {
			set(key, value);
		}
	}

	/**
	 * Gets an integer config value
	 * 
	 * @param key The key of the pair
	 * @param defaultValue The default value for the pair if it doesn't exist
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
	 * @param defaultValue The default value for the pair if it doesn't exist
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
	 * @param defaultValue The default value for the pair if it doesn't exist
	 * @return The value by the given key
	 */
	public String getString(String key) {
		return get(key);
	}

	public String getTempFolder() {
		return folder;
	}
	
	public File getTorrentFileFor(Torrent torrent) {
		return new File(String.format("%s%s.torrent", getTempFolder(), torrent.getHash()));
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
	
	private final String getMissingConfigError(String key) {
		return String.format("Configuration Setting \"%s\" has not been registered", key);
	}

}
