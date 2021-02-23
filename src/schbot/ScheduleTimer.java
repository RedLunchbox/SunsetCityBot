package schbot;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import discord4j.common.util.Snowflake;
import sch.discord.SchedulingHandler;

public class ScheduleTimer {
	
	public static ScheduleTimer construct(TimeIncrement ti, SchedulePost post, Snowflake UserID)
	{
		return new ScheduleTimer(ti,post.getDate(),post.getPostID(),UserID,post.getTitle());
	}
	
	private ScheduleTimer(TimeIncrement ti, Date d, Snowflake postID, Snowflake userID, String title) {
		this.postID = postID;
		this.userID = userID;
		this.title = title;
		this.timeIncrement = ti;
		this.timer = new Timer();
		int calendarValue;
		if (ti.getIncrement().equals(ValidTimeUnits.DAY))
			calendarValue = Calendar.DATE;
		else if(ti.getIncrement().equals(ValidTimeUnits.MINUTE))
			calendarValue = Calendar.MINUTE;
		else calendarValue = Calendar.HOUR;
		
		Calendar gc = new GregorianCalendar();
		gc.setTime(d);
		gc.add(calendarValue, -ti.getValue());
		endDate = gc.getTime();
	}

	private final Timer timer;
	private final Snowflake postID;
	private final Snowflake userID;
	private final String title;
	private final Date endDate;
	private final TimeIncrement timeIncrement;
	

	public Timer getTimer() {
		return timer;
	}
	
	public boolean isAfterNow()
	{
		return (endDate.compareTo(new Date()) > 0);
	}
	
	public void start()
	{
		ScheduleTimer t= this;
		 TimerTask task = new TimerTask() {
		        @Override
				public void run() {
		        	SchedulingHandler.timerOnOff(t);
		        }
		    };
		    timer.schedule(task, endDate);
	}
	
	public String formatTimeIncrement()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(timeIncrement.value);
		sb.append(" ");
		sb.append(timeIncrement.increment.toString().toLowerCase());
		if (timeIncrement.value!=1)
		{
			sb.append("s");
		}
		return sb.toString();
	}

	public Snowflake getUserID() {
		return userID;
	}

	public Snowflake getPostID() {
		return postID;
	}

	public String getTitle() {
		return title;
	}

	public TimeIncrement getTimeIncrement() {
		return timeIncrement;
	}
	
	public boolean checkPostAndCancel(Snowflake postID)
	{
		if (postID.equals(this.postID))
		{
			timer.cancel();
			return true;
		}
		return false;
	}
	
	public boolean checkUserAndCancel(Snowflake postID)
	{
		if (userID.equals(this.postID))
		{
			timer.cancel();
			return true;
		}
		return false;
	}


	public Date getEndDate() {
		return endDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime * result + ((postID == null) ? 0 : postID.hashCode());
		result = prime * result + ((userID == null) ? 0 : userID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScheduleTimer other = (ScheduleTimer) obj;
		if (endDate == null) {
			if (other.endDate != null)
				return false;
		} else if (!endDate.equals(other.endDate))
			return false;
		if (postID == null) {
			if (other.postID != null)
				return false;
		} else if (!postID.equals(other.postID))
			return false;
		if (userID == null) {
			if (other.userID != null)
				return false;
		} else if (!userID.equals(other.userID))
			return false;
		return true;
	}

	public boolean checkPostAuthorIncrementAndCancel(Snowflake authorID, TimeIncrement ti) {
		if (userID.equals(this.postID))
		{
			if (ti.equals(this.timeIncrement))
			{
				timer.cancel();
				return true;
			}
		}
		return false;
	}


}
