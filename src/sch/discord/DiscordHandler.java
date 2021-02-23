package sch.discord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Permission;
import gretchen.Gretchen;
import reactor.core.publisher.Mono;
import schbot.CharacterStatus;
import schbot.KickTicket;
import schbot.SnowflakeLinkRequest;
import schbot.UIContainer;

public class DiscordHandler {

	static DiscordClient client;
	static GatewayDiscordClient gateway;
	public static UIContainer UIContainer;
	public static final String DISCORD_TOKEN = "cfg/discord.token";

	public static final String ROLE_TICKET_PATH = "data/roletickets.dat";
	public static final String SNOWFLAKE_TICKET_PATH = "data/snowflaketickets.dat";

	private static Map<Snowflake, KickTicket> roleMap = new HashMap<>();

	private static Map<Snowflake, Snowflake> replyMap = new HashMap<>();
	private static LinkedList<Snowflake> replyQueue = new LinkedList<>();
	private static Map<Snowflake,SnowflakeLinkRequest> snowflakeRequestTickets = new HashMap<>();
	private static int MAX_REPLYMAP_SIZE = 256;

	public static final int DISCORD_CHAR_LIMIT = 2000;

	public static final Snowflake SCRATCHPAD_SNOWFLAKE = Snowflake.of("736476986725236756");

	public static void init() {
		// Log into Discord
		long startTime = System.nanoTime();
		String token = null;
		try {
			Scanner in = new Scanner(new File(DISCORD_TOKEN));
			token = in.nextLine();
			in.close();
		} catch (FileNotFoundException e1) {
			UIContainer.displayError(e1.getMessage());
			System.exit(0);
		}
		client = DiscordClient.create(token);
		gateway = client.login().block();

		// Set up Role Retention
		gateway.on(MemberLeaveEvent.class).subscribe(event -> {
			System.out.println("Member left.");
			if (event.getGuild().block().getAuditLog().onErrorResume(err -> Mono.empty()).blockFirst().getActionType().equals(ActionType.MEMBER_KICK)) {
				System.out.println("Member was kicked.");
			}  else {
				roleMap.put(event.getMember().get().getId(), new KickTicket(event));
				saveRoleTickets();
			}
		});

		gateway.on(MemberJoinEvent.class).subscribe(event -> {
			Member m = event.getMember();
			if (roleMap.containsKey(m.getId())) {
				KickTicket t = roleMap.get(m.getId());
				if (!isTicketValid(t)) {
					System.out.println("Ticket invalid, removing");
					roleMap.remove(m.getId());
					return;
				}
				System.out.println("Person came back!");
				Set<Snowflake> roles = t.getRoles();
				System.out.println("Set Size: " + roles.size());
				Iterator<Snowflake> i = roles.iterator();
				while (i.hasNext()) {
					m.addRole(i.next()).onErrorResume(err -> Mono.empty()).block();
				}
				roleMap.remove(m.getId());
				saveRoleTickets();
			}
		});

		gateway.on(ReadyEvent.class).subscribe(event -> {
			User self = event.getSelf();
			System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
		});
		
		Messages.init();
		RegistryHandler.init();
		SchedulingHandler.init();
		LFGHandler.executeLFGMonitoring();
		try {
			Gretchen.LoadMemoryFile();
		} catch (FileNotFoundException e) {
			UIContainer.displayError(e.getMessage());
		}
		CommandHandler.executeCommandHandling();
		AutoPruning.init();
		loadRoleTickets();
		loadSnowflakeRequests();

		DiscordHandler.gateway.on(MessageDeleteEvent.class).subscribe(event -> {

			Snowflake sf = event.getMessageId();
			if (replyMap.containsKey(sf)) {
				Message m = gateway.getMessageById(event.getChannelId(), replyMap.get(sf))
						.onErrorResume(err -> Mono.empty()).block();
				if (m == null)
					return;
				m.delete().block();
				replyMap.remove(sf);
				replyQueue.remove(sf);
			}

		});

		long stopTime = System.nanoTime();
		double measure = (stopTime - startTime) / 1000000000;
		post(SCRATCHPAD_SNOWFLAKE,"Successfully Booted Up ("+measure+"s)");

		System.out.println("Ready");
		gateway.onDisconnect().block();
	}

	static boolean isAdmin(Member member) {
		return checkPermissions(member, EnumSet.of(Permission.ADMINISTRATOR));
	}
	
	

	public static boolean isTicketValid(KickTicket ticket) {
		Calendar c = new GregorianCalendar();
		c.setTime(ticket.getDateLeft());
		c.add(GregorianCalendar.WEEK_OF_YEAR, 1);
		if (c.compareTo(new GregorianCalendar()) < 0) {
			return false;
		} else {
			return true;
		}
	}

	public static void clearTickets() {
		roleMap.values().removeIf(value -> DiscordHandler.isTicketValid(value));
		saveRoleTickets();
	}

	public static void saveRoleTickets() {
		try {
			System.out.println("Saving Tickets");
			Iterator<Snowflake> i;
			ObjectOutputStream eout = new ObjectOutputStream(new FileOutputStream(ROLE_TICKET_PATH));
			eout.writeInt(roleMap.size());
			for (Entry<Snowflake, KickTicket> entry : roleMap.entrySet()) {
				KickTicket ticket = entry.getValue();
				eout.writeLong(entry.getKey().asLong());
				eout.writeObject(ticket.getDateLeft());
				eout.writeInt(ticket.getRoles().size());
				i = ticket.getRoles().iterator();
				while (i.hasNext()) {
					eout.writeLong(i.next().asLong());
				}
			}
			eout.close();
			System.out.println("Tickets Saved");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createSnowflakePairRequest(Member member, CharacterStatus requestedStatus, List<Object> row)
	{
		String messageText = "Beep Boop!";
		MessageData m = post(SCRATCHPAD_SNOWFLAKE,messageText);
		SnowflakeLinkRequest request = new SnowflakeLinkRequest(member.getId(), requestedStatus, row);
		snowflakeRequestTickets.put(Snowflake.of(m.id()),request);
		saveSnowflakeRequests();
	}
	
	public static void saveSnowflakeRequests()
	{
		ObjectOutputStream eout;
		try {
			eout = new ObjectOutputStream(new FileOutputStream(SNOWFLAKE_TICKET_PATH));
			eout.writeInt(snowflakeRequestTickets.size());
			for (Entry<Snowflake, SnowflakeLinkRequest> entry : snowflakeRequestTickets.entrySet()) {
				eout.writeLong(entry.getKey().asLong());
				SnowflakeLinkRequest r=  entry.getValue();
				eout.writeLong(r.getUserId().asLong());
				eout.writeUTF(r.getStatusRequest().name());
				List<Object> row = r.getRow();
				eout.writeInt(row.size());
				for (int x=0;x<row.size();x++)
				{
					eout.writeUTF(row.get(x).toString());
				}
				
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
	
	public static void loadSnowflakeRequests()
	{
		try {
			ObjectInputStream ein = new ObjectInputStream(new FileInputStream(SNOWFLAKE_TICKET_PATH));
			int numberOfTickets = ein.readInt();
			Snowflake postID;
			Snowflake userID;
			CharacterStatus status;
			int rowListLength;
			List<Object> row;
			SnowflakeLinkRequest request;
			
			for (int x=0;x<numberOfTickets;x++)
			{
				postID= Snowflake.of(ein.readLong());
				userID= Snowflake.of(ein.readLong());
				status = CharacterStatus.valueOf(ein.readUTF());
				rowListLength=ein.readInt();
				row = new ArrayList<>();
				for (int y=0;y<rowListLength;y++)
				{
					row.add(ein.readUTF());
				}
				request = new SnowflakeLinkRequest(userID, status, row);
				snowflakeRequestTickets.put(postID, request);
				
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

	public static void loadRoleTickets() {
		System.out.println("Loading Tickets");
		Snowflake memberID;
		Set<Snowflake> roles = new HashSet<>();
		int numberOfRoles;
		KickTicket ticket;
		try {
			ObjectInputStream ein = new ObjectInputStream(new FileInputStream(ROLE_TICKET_PATH));
			int numberOfTickets = ein.readInt();
			for (int x = 0; x < numberOfTickets; x++) {
				ticket = new KickTicket();
				memberID = Snowflake.of(ein.readLong());
				ticket.setDateLeft((Date) ein.readObject());
				numberOfRoles = ein.readInt();
				roles.clear();
				for (int y = 0; y < numberOfRoles; y++) {
					roles.add(Snowflake.of(ein.readLong()));
				}
				ticket.setRoles(roles);
				roleMap.put(memberID, ticket);
			}
			ein.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean checkPermissions(Member m, EnumSet<Permission> permissions) {
		m.getBasePermissions().block();
		discord4j.rest.util.PermissionSet s = m.getBasePermissions().block();
		Iterator<discord4j.rest.util.Permission> i = permissions.iterator();
		while (i.hasNext()) {
			if (s.contains(i.next())) {
				return true;
			}
		}
		return false;
	}

	static MessageData post(Snowflake channel, String content) {
		return gateway.getChannelById(channel).block().getRestChannel().createMessage(content)
				.onErrorResume(err -> Mono.empty()).block();
	}

	static void replyPrivately(MessageCreateEvent event, String reply) {
		System.out.println("Replying: " + reply);
		event.getMember().get().getPrivateChannel().block().createMessage(reply).block();
	}

	static void reply(MessageCreateEvent event, String reply) {
		System.out.println("Replying: " + reply);
		if (reply.length() > DISCORD_CHAR_LIMIT) {
			System.out.println("Reply Exceeds Discord Character Limit");
			event.getMessage().getChannel().block()
					.createMessage("ERROR: Tried sending a message that exceeded discord's character limit").block();
			return;
		}

		Message botMessage = event.getMessage().getChannel().block().createMessage(reply).block();
		addToReplyMap(event.getMessage().getId(), botMessage.getId());
	}

	public static void addToReplyMap(Snowflake message, Snowflake reply) {
		replyMap.put(message, reply);
		replyQueue.add(message);
		if (replyQueue.size() > MAX_REPLYMAP_SIZE) {
			replyMap.remove(replyQueue.pop());
		}

	}

	public static void sendMessage(Snowflake UserID, String reply) {
		System.out.println("Replying: " + reply);
		client.getUserById(UserID).getPrivateChannel().block();
		User user = gateway.getUserById(UserID).onErrorResume(err -> Mono.empty()).block();
		if (user == null) {
			return;
		}
		if (user.getPrivateChannel().block().createMessage(reply).onErrorResume(err -> Mono.empty()).block() == null) {
			System.out.println("Message Failed to Send");
		}
	}

	public static void replyPrivately(Member member, String reply) {
		System.out.println("Replying: " + reply);
		member.getPrivateChannel().block().createMessage(reply).block();

	}

	public void logout() {
		gateway.logout().block();
	}
}
