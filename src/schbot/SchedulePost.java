package schbot;

import java.util.Date;
import java.util.List;

import discord4j.common.util.Snowflake;

public class SchedulePost {
	
	private Date date;
	private Snowflake Gm;
	private Snowflake postID;
	private List<Snowflake> mentions;
	private String gmUsername;
	private String title;
	private boolean hideTime=false;
	private boolean valid=true;
	
	public SchedulePost()
	{
		
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Snowflake getGm() {
		return Gm;
	}

	public void setGm(Snowflake gm) {
		Gm = gm;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "SchedulePost [date=" + date + ", Gm=" + Gm + ", title=" + title + "]";
	}

	public boolean isHideTime() {
		return hideTime;
	}

	public void setHideTime(boolean hideTime) {
		this.hideTime = hideTime;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public Snowflake getPostID() {
		return postID;
	}

	public void setPostID(Snowflake postID) {
		this.postID = postID;
	}

	public List<Snowflake> getMentions() {
		return mentions;
	}

	public void setMentions(List<Snowflake> mentions) {
		this.mentions = mentions;
	}

	public String getGmUsername() {
		return gmUsername;
	}

	public void setGmUsername(String gmUsername) {
		this.gmUsername = gmUsername;
	}


}
