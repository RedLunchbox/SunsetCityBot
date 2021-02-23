package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Flux;

public class AutoPruning {

//	private static List<PruneSchedule> pruneSchedule;
	private static Snowflake TEMP_HARDCODED_VENTING_SNOWFLAKE;
	
	/*
	 * This is temporarily hard coded in place in order for the bot to be functional.
	 * Ideally, admins should be able to set what time the prunes should happen on what channels.
	 */
	private static final int TEMP_HARDCODED_PRUNE_HOUR=4;
	private static PruneSchedule schedule;
	public static PruneSchedule getSchedule() {
		return schedule;
	}

	static final String PROPERTIES_PATH = "cfg/vent.txt";
	
	
	
	public static void init()
	{
		loadData();
		createTimer(schedule);
	}
	
	public static void loadData()
	{
		Properties properties = new Properties();
			try {
				properties.load(new FileInputStream(PROPERTIES_PATH));
				TEMP_HARDCODED_VENTING_SNOWFLAKE = Snowflake.of(properties.getProperty("VENTING_SNOWFLAKE"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			schedule = new PruneSchedule(TEMP_HARDCODED_VENTING_SNOWFLAKE,TEMP_HARDCODED_PRUNE_HOUR);
	}

	public static void doAutoPrune(PruneSchedule pn) {
		System.out.println("Starting test");
		Channel c = DiscordHandler.gateway.getChannelById(pn.getChannelSnowflake()).block();
		List<MessageData> mList = c.getRestChannel().getMessagesAfter(Snowflake.of(0)).buffer(1000).blockFirst();
		List<Snowflake> sList = new LinkedList<>();
		for (int x=0;x<mList.size();x++)
		{
			if (!mList.get(x).pinned())
			{
				sList.add(Snowflake.of(mList.get(x).id()));
			}
		}
		System.out.println((sList.size()));
		
		c.getRestChannel().bulkDelete(Flux.fromIterable(sList)).buffer().blockFirst(Duration.ofSeconds(5));
		
		//Handle clleaning out RoleTickets while we're here
		DiscordHandler.clearTickets();
	}
	
	
	public static void createTimer(PruneSchedule pn)
	{
		System.out.println("Setting Up Timer");
		TimerTask task = new TimerTask() {
	        @Override
			public void run() {
	        	doAutoPrune(pn);
	        	createTimer(pn);
	        }
	    };
	    new Timer().schedule(task, pn.getScheduledTime());
	}

}
