package schbot;

import java.util.List;

import discord4j.common.util.Snowflake;

public class SnowflakeLinkRequest {
	
	private final Snowflake userId;
	private final CharacterStatus statusRequest;
	private final List<Object> row;
	
	public SnowflakeLinkRequest(Snowflake userId, CharacterStatus statusRequest, List<Object> row2) {
		super();
		this.userId = userId;
		this.statusRequest = statusRequest;
		this.row = row2;
	}
	public List<Object> getRow() {
		return row;
	}
	public CharacterStatus getStatusRequest() {
		return statusRequest;
	}
	public Snowflake getUserId() {
		return userId;
	}

}
