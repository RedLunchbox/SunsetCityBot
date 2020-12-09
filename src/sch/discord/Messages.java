package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public class Messages {
	
	public static String LACKS_PERMISSION_GENERIC;
	public static String LACKS_PERMISSION_EMOJI;
	
	public static String LFG_NOT_GM_MESSAGE;
	public static String LFG_SET_CHANNEL_SUCCESS;
	public static String LFG_SET_GM_FAIL_NO_ROLE_MENTIONED;
	public static String LFG_SET_GM_SUCCESS;
	public static String LFG_EMOJI_CLEAR_SUCCESS;
	
	public static String REGISTRY_CHARACTERSEARCHBYNAME_FAIL_NO_CHARACTER_MENTIONED;
	public static String REGISTRY_CHARACTERSEARCHBYNAME_RETURNED_MULTIPLE_SHEETS;
	public static String REGISTRY_CHARACTERSEARCHBYNAME_NO_CHARACTER_FOUND;
	public static String REGISTRY_CHARACTERSEARCHBYNAME_RETURNED_MULTIPLE_SHEETS_TOO_MANY;
	public static String REGISTRY_FAILURE_BACKEND;
	
	public static String SCHEDULE_TIMEZONE_INFO;
	
	public static String SCHEDULE_REMINDER_MESSAGE;
	public static String SCHEDULE_REMINDER_CLEARED;
	public static String SCHEDULE_REMINDER_ERROR_NONE;
	public static String SCHEDULE_REMINDER_ERROR_MAX;
	public static String SCHEDULE_REMINDER_SHOW;
	
	public static String SCHEDULE_REMINDER_ADD_ERROR_INVALID_FORMAT;
	public static String SCHEDULE_REMINDER_ADD;
	public static String SCHEDULE_REMINDER_ERROR_FUCK_YOU;
	public static String SCHEDULE_REMINDER_REMOVE_ERROR_INVALID_FORMAT;
	public static String SCHEDULE_REMINDER_REMOVE_SUCCESS;
	public static String SCHEDULE_REMINDER_REMOVE_ERROR_DOES_NOT_EXIST;
	
	static {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("cfg/messages.txt"));
		} catch (FileNotFoundException e) {
			DiscordHandler.UIContainer.displayError(e.getMessage());
		} catch (IOException e) {
			DiscordHandler.UIContainer.displayError(e.getMessage());
		}
		
		Field[] fields = Messages.class.getFields();
		System.out.println(fields.length);
		for (int x=0;x<fields.length;x++)
		{
			try {
				fields[x].set(null, properties.getProperty(fields[x].getName()));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//Blank method used to give the above static into gear.
	public static void init()
	{
		
	}

}
