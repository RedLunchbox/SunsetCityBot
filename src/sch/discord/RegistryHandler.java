package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import apis.GoogleSheets;
import discord4j.common.util.Snowflake;

public class RegistryHandler {

	private static boolean registryOnline = false;
	private static final String PROPERTIES_PATH = "cfg/googlesheets.txt";
	private static final String SNOWFLAKELINKPATH = "data/registrysnowflakelink.dat";

	private static Map<String, Snowflake> registryNameSnowFlakeLink = new HashMap<>();

	public static void init() {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTIES_PATH));
			GoogleSheets.SpreadSheetID = properties.getProperty("SheetID");
			GoogleSheets.range = properties.getProperty("RANGE");
			GoogleSheets.CHARACTER_NAME_ROW = Integer.parseInt(properties.getProperty("CHARACTER_NAME_ROW"));
			GoogleSheets.CHARACTER_SHEET_LINK_ROW = Integer
					.parseInt(properties.getProperty("CHARACTER_SHEET_LINK_ROW"));
		} catch (FileNotFoundException e1) {
			System.err.println("GOOGLE SHEETS PROPERTIES FILE NOT FOUND: " + PROPERTIES_PATH);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			DiscordHandler.UIContainer.displayError(e1.getMessage());
		}
		try {
			GoogleSheets.connect();
			registryOnline = true;
		} catch (GeneralSecurityException | IOException e) {
			// TODO Auto-generated catch block
			DiscordHandler.UIContainer.displayError("Error Connecting to Google Sheets: " + e.getMessage());
		}
		if (registryOnline) {
			loadRegistryNameSnowflakeLink();
		}
	}

	private static void loadRegistryNameSnowflakeLink() {
		try {
			ObjectInputStream ein = new ObjectInputStream(new FileInputStream(SNOWFLAKELINKPATH));
			int iterations = ein.readInt();
			String key;
			Snowflake value;
			for (int x = 0; x < iterations; x++) {
				key = ein.readUTF();
				value = Snowflake.of(ein.readLong());
				registryNameSnowFlakeLink.put(key, value);
			}
			ein.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static void saveRegistryNameSnowflakeLink() {
		try {
			ObjectOutputStream eout = new ObjectOutputStream(new FileOutputStream(SNOWFLAKELINKPATH));
			eout.writeInt(registryNameSnowFlakeLink.size());
			for (Map.Entry<String, Snowflake> entry : registryNameSnowFlakeLink.entrySet()) {
				eout.writeUTF(entry.getKey());
				eout.writeLong(entry.getValue().asLong());
			}
			eout.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static boolean isNameLinkedWithSnowflake(String name)
	{
		return registryNameSnowFlakeLink.containsKey(name);
	}
	
	public static boolean isRegistryOnline() {
		return registryOnline;
	}

}
