package org.johnnei.utils.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.johnnei.utils.ThreadUtils;

public class Config {
	
	/**
	 * The singleton config instance
	 */
	private static Config instance;
	
	/**
	 * Gets the singleton config file for JavaTorrent
	 * @return
	 */
	public static Config getConfig() {
		if(instance == null)
			instance = new Config();
		return instance;
	}
	
	/**
	 * The file which the config is being stored
	 */
	private File configFile;
	/**
	 * All configs which are located in the config file
	 */
	private HashMap<String, String> config;
	
	private Config() {
		getFile("JavaTorrent.cfg");
		config = new HashMap<>();
	}
	
	private void getFile(String filename) {
		String absolutePath = System.getProperty("user.home") + "\\";
		String os = System.getProperty("os.name");
		if(os.equals("Windows 7") || os.equals("Windows Vista")) {
			absolutePath += "AppData\\Roaming\\JavaTorrent\\";
			new File(absolutePath).mkdirs();
		}
		configFile = new File(absolutePath + filename);
		if(!configFile.exists()) {
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
			 while((line = inputStream.readLine()) != null) {
				 if(line.trim().length() > 0 && line.contains("=")) {
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
			if(inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {}
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
			for(Entry<String, String> entry : config.entrySet()) {
				outStream.write(entry.getKey() + "=" + entry.getValue() + "\n");
			}
		} catch (IOException e) {
			ThreadUtils.sleep(10);
			save();
		} finally {
			if(outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {}
			}
		}
	}
	
	/**
	 * Adds a key/value pair to the config file
	 * @param key The key of the pair
	 * @param value The value of the pair
	 */
	private void add(String key, Object value) {
		config.put(key, value.toString());
		save();
	}
	
	/**
	 * Gets a value from the config
	 * @param key The key of the pair
	 * @param defaultValue The default value for the pair if it doesn't exist
	 * @return The value by the given key
	 */
	private String get(String key, Object defaultValue) {
		String value = config.get(key);
		if(value == null) {
			add(key, defaultValue);
			return defaultValue.toString();
		} else {
			return value;
		}
	}
	
	/**
	 * Overrides a pair of values
	 * @param key The key of the pair
	 * @param value The value of the pair
	 */
	public void set(String key, Object value) {
		config.put(key, value.toString());
		save();
	}
	
	/**
	 * Gets an integer config value
	 * @param key The key of the pair
	 * @param defaultValue The default value for the pair if it doesn't exist
	 * @return The value by the given key
	 */
	public int getInt(String key, int defaultValue) {
		return Integer.parseInt(get(key, defaultValue));
	}

}
