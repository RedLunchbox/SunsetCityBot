package schbot;

import java.util.Date;
import java.util.Set;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberLeaveEvent;

public class KickTicket {
	
	private Date dateLeft;
	private Set<Snowflake> roles;
	
	public KickTicket()
	{
		
	}
	
	public KickTicket(MemberLeaveEvent event)
	{
		setDateLeft(new Date());
		setRoles(event.getMember().get().getRoleIds());
	}

	public Set<Snowflake> getRoles() {
		return roles;
	}

	public void setRoles(Set<Snowflake> roles) {
		this.roles = roles;
	}

	public Date getDateLeft() {
		return dateLeft;
	}

	public void setDateLeft(Date dateLeft) {
		this.dateLeft = dateLeft;
	}
}
