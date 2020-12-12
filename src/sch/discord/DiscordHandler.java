package sch.discord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import gretchen.Gretchen;
import reactor.core.publisher.Mono;
import schbot.UIContainer;

public class DiscordHandler {

	static DiscordClient client;
	static GatewayDiscordClient gateway;
	public static UIContainer UIContainer;
	public static final String DISCORD_TOKEN = "cfg/discord.token";

	private static Map<Snowflake, Snowflake> replyMap = new HashMap<>();
	private static LinkedList<Snowflake> replyQueue = new LinkedList<>();
	private static int MAX_REPLYMAP_SIZE = 256;

	public static final int DISCORD_CHAR_LIMIT = 2000;

	public static void init() {
		// Log into Discord

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
		System.out.println("Ready");
		gateway.onDisconnect().block();
	}

	static boolean isAdmin(Member member) {
		return checkPermissions(member, EnumSet.of(Permission.ADMINISTRATOR));
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
