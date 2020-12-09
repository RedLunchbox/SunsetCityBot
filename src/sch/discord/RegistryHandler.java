package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import apis.GoogleSheets;

public class RegistryHandler {
	
	private static boolean registryOnline=false;
	private static final String PROPERTIES_PATH="cfg/googlesheets.txt";
	
	public static void init()
	{
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTIES_PATH));
			GoogleSheets.SpreadSheetID = properties.getProperty("SheetID");
			GoogleSheets.range = properties.getProperty("RANGE");
			GoogleSheets.CHARACTER_NAME_ROW = Integer.parseInt(properties.getProperty("CHARACTER_NAME_ROW"));
			GoogleSheets.CHARACTER_SHEET_LINK_ROW = Integer.parseInt(properties.getProperty("CHARACTER_SHEET_LINK_ROW"));
		} catch (FileNotFoundException e1) {
			System.err.println("GOOGLE SHEETS PROPERTIES FILE NOT FOUND: "+PROPERTIES_PATH);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			DiscordHandler.UIContainer.displayError(e1.getMessage());
		}
		try {
			GoogleSheets.connect();
			registryOnline=true;
		} catch (GeneralSecurityException | IOException e) {
			// TODO Auto-generated catch block
			DiscordHandler.UIContainer.displayError("Error Connecting to Google Sheets: "+e.getMessage());
		}
	}
	
	public static boolean isRegistryOnline()
	{
		return registryOnline;
	}

}
