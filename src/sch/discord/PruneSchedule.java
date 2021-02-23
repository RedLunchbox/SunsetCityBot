package sch.discord;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import discord4j.common.util.Snowflake;

public class PruneSchedule {
	
	private Snowflake channelSnowflake;
	private int hour;
	
	public PruneSchedule(Snowflake vENTING_SNOWFLAKE, int i) {
		channelSnowflake = vENTING_SNOWFLAKE;
		setHour(i);
	}
	public PruneSchedule() {
		// TODO Auto-generated constructor stub
	}
	public Snowflake getChannelSnowflake() {
		return channelSnowflake;
	}
	public void setChannelSnowflake(Snowflake channelSnowflake) {
		this.channelSnowflake = channelSnowflake;
	}
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	
	public Date getScheduledTime()
	{
		Calendar gc = new GregorianCalendar();
		gc.set(Calendar.HOUR_OF_DAY, hour);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		if (gc.compareTo(new GregorianCalendar())<0)
		{
			System.out.println("After time, increasing a day");
			gc.add(Calendar.DAY_OF_YEAR, 1);
		}
		return gc.getTime();
	}

}
