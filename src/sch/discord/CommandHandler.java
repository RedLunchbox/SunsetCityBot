package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import apis.GoogleSheets;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import gretchen.Gretchen;
import reactor.core.publisher.Mono;
import schbot.SchedulePost;
import schbot.TimeIncrement;
import schbot.ValidTimeUnits;

public class CommandHandler {

	public static final Map<String, Command> commands = new HashMap<>();
	static String commandPrefix = "$";

	static {
		System.out.println("Loading Commands");
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("cfg/commands.txt"));
		} catch (FileNotFoundException e) {
			System.err.println("FILE NOT FOUND: cfg/commands.txt");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		commandPrefix = properties.getProperty("COMMAND_PREFIX");

		commands.put(properties.getProperty("TEST_PING_COMMAND"), event -> {
			event.getMessage().getChannel().block().createMessage("Pong!").block();
		});
		commands.put(properties.getProperty("GM_SET_COMMAND"), event -> {
			doCommandSetGMRole(event);
		});
		commands.put(properties.getProperty("LFG_CHANNEL_SET_COMMAND"), event -> {
			doCommandSetLFGChannel(event);
		});
		commands.put(properties.getProperty("CHARACTER_SEARCH_BY_NAME_COMMAND"), event -> {
			doSearchCharacter(event);
		});
		commands.put(properties.getProperty("FUNNYBOT_COMMAND"), event -> {
			doFunnyGretchenResponse(event);
		});
		commands.put(properties.getProperty("LFG_CLEAR_EMOJI_COMMAND"), event -> {
			doClearLFGEmojis(event);
		});
		commands.put(properties.getProperty("SCHEDULING_POST_CHRONOLOGICAL_COMMAND"), event -> {
			doPostScheduleChronological(event);
		});

		commands.put(properties.getProperty("SCHEDULING_REMINDER_ADD_COMMAND"), event -> {
			doSchedulingReminderAdd(event);
		});
		commands.put(properties.getProperty("SCHEDULING_REMINDER_CLEAR_COMMAND"), event -> {
			doSchedulingReminderClear(event);
		});
		commands.put(properties.getProperty("SCHEDULING_REMINDER_SHOW_COMMAND"), event -> {
			doSchedulingReminderList(event);
		});
		commands.put(properties.getProperty("SCHEDULING_REMIDNER_REMOVE_COMMAND"), event -> {
			doSchedulingReminderRemove(event);
		});
		
		//ALPHA
		commands.put(properties.getProperty("SEARCH_WIKI_COMMAND"), event -> {
			doWikiSearch(event, properties.getProperty("SEARCH_WIKI_COMMAND"));
		});
		
		
		commands.put("snowflake", event -> {
			StringBuilder sb = new StringBuilder();
			sb.append("Server ID: ");
			sb.append(event.getGuildId().get().asLong());
			sb.append("\nChannel ID: ");
			sb.append(event.getMessage().getChannelId().asLong());
			DiscordHandler.reply(event, sb.toString());
		});
		commands.put("pruneschedule", event -> {
			DiscordHandler.reply(event, "Next Venting Auto-Prune: "+AutoPruning.getSchedule().getScheduledTime());
		});


		LFGHandler.setLFGEmojiCommand = properties.getProperty("LFG_EMOJI_SET_COMMAND");
	}
	
	
	private static void doWikiSearch(MessageCreateEvent event, String prefix)
	{
		String message = event.getMessage().getContent();
		message = message.substring(prefix.length()+commandPrefix.length());
		System.out.println(message);
		String response = WikiSearcher.searchFor(message);
		if (response !=null)
		{
			DiscordHandler.reply(event, response);
		} else
			DiscordHandler.reply(event, "No results found.");
	}

	private static void doClearLFGEmojis(MessageCreateEvent event) {
		if (DiscordHandler.isAdmin(event.getMember().get())) {
			LFGHandler.lfgEmojis = new HashSet<>();
			LFGHandler.saveData();
			DiscordHandler.reply(event, Messages.LFG_EMOJI_CLEAR_SUCCESS);
		} else {
			DiscordHandler.reply(event, Messages.LACKS_PERMISSION_GENERIC);
		}

	}

	private static void doSchedulingReminderList(MessageCreateEvent event) {
		Snowflake userID = event.getMessage().getAuthor().get().getId();
		if (SchedulingHandler.getPeopletoremind().containsKey(userID)) {
			Set<TimeIncrement> timeList = SchedulingHandler.getPeopletoremind().get(userID);
			Iterator<TimeIncrement> i = timeList.iterator();
			StringBuilder sb = new StringBuilder();
			while (i.hasNext()) {
				sb.append("\n-");
				TimeIncrement ti = i.next();
				sb.append(ti.toMessageString());
			}
			DiscordHandler.reply(event, String.format(Messages.SCHEDULE_REMINDER_SHOW, timeList.size(), sb.toString()));
		} else {
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_NONE);
		}

	}

	public static boolean isValidInteger(String str) {
		if (str.length() == 0) {
			return false;
		}
		for (char c : str.toCharArray()) {
			if (!Character.isDigit(c))
				return false;
		}
		return true;
	}

	private static void doSchedulingReminderClear(MessageCreateEvent event) {
		Snowflake userID = event.getMessage().getAuthor().get().getId();
		if (SchedulingHandler.getPeopletoremind().containsKey(userID)) {
			SchedulingHandler.clearReminders(userID);
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_CLEARED);
		} else {
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_NONE);
		}

	}

	private static void doSchedulingReminderRemove(MessageCreateEvent event) {
		Snowflake authorID = event.getMessage().getAuthor().get().getId();
		if (!SchedulingHandler.getPeopletoremind().containsKey(authorID)) {
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_NONE);
			return;
		}
		Set<TimeIncrement> reminders = SchedulingHandler.getPeopletoremind().get(authorID);

		System.out.println("Removing User");
		String message = event.getMessage().getContent();
		String[] words = message.split(" ");
		System.out.println("String Split");
		if (words.length == 3) {
			if (isValidInteger(words[1])) {
				if (words[1].length() >= 10) {
					DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_FUCK_YOU);
					return;
				}
				int value = Integer.valueOf(words[1]);
				ValidTimeUnits tu = null;
				String timeUnitCheck = words[2].toUpperCase();
				ValidTimeUnits[] units = ValidTimeUnits.values();
				for (int x = 0; x < units.length; x++) {
					if (timeUnitCheck.contains(units[x].toString())) {
						tu = units[x];
					}
				}
				if (tu != null) {
					TimeIncrement ti = new TimeIncrement(value, tu);
					if (reminders.contains(ti)) {
						SchedulingHandler.removeReminder(authorID, ti);
						DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_REMOVE_SUCCESS);
					} else {
						DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_REMOVE_ERROR_DOES_NOT_EXIST);
					}
				} else {
					DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_REMOVE_ERROR_INVALID_FORMAT);
				}
			} else {
				DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_REMOVE_ERROR_INVALID_FORMAT);
			}
		} else {
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_REMOVE_ERROR_INVALID_FORMAT);
		}

	}

	private static void doSchedulingReminderAdd(MessageCreateEvent event) {
		Snowflake authorID = event.getMessage().getAuthor().get().getId();
		if (SchedulingHandler.getPeopletoremind().containsKey(authorID)) {
			if (SchedulingHandler.getPeopletoremind().get(authorID).size() >= SchedulingHandler.maximum_reminders) {
				DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_MAX);
				return;
			}
		}

		System.out.println("Adding User");
		String message = event.getMessage().getContent();
		String[] words = message.split(" ");
		System.out.println("String Split");
		if (words.length == 3) {
			if (isValidInteger(words[1])) {
				if (words[1].length() >= 10) {
					DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ERROR_FUCK_YOU);
					return;
				}
				int value = Integer.valueOf(words[1]);
				ValidTimeUnits tu = null;
				String timeUnitCheck = words[2].toUpperCase();
				ValidTimeUnits[] units = ValidTimeUnits.values();
				for (int x = 0; x < units.length; x++) {
					if (timeUnitCheck.contains(units[x].toString())) {
						tu = units[x];
					}
				}
				if (tu != null) {
					SchedulingHandler.addReminder(value, tu, authorID);
					DiscordHandler.reply(event,
							String.format(Messages.SCHEDULE_REMINDER_ADD,
									new TimeIncrement(value, tu).toMessageString(),
									SchedulingHandler.getPeopletoremind().get(authorID).size()));
				} else {
					DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ADD_ERROR_INVALID_FORMAT);
				}
			} else {
				DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ADD_ERROR_INVALID_FORMAT);
			}
		} else {
			DiscordHandler.reply(event, Messages.SCHEDULE_REMINDER_ADD_ERROR_INVALID_FORMAT);
		}

	}

	static void doCommandSetGMRole(MessageCreateEvent event) {
		if (!DiscordHandler.isAdmin(event.getMember().get())) {
			DiscordHandler.reply(event, Messages.LACKS_PERMISSION_GENERIC);
			return;
		}
		Set<Snowflake> mentionedRoles = event.getMessage().getRoleMentionIds();
		if (mentionedRoles.size() <= 0) {
			DiscordHandler.reply(event, Messages.LFG_SET_GM_FAIL_NO_ROLE_MENTIONED);
			return;
		}
		LFGHandler.gmRole = event.getMessage().getRoleMentionIds().iterator().next();
		DiscordHandler.reply(event, Messages.LFG_SET_GM_SUCCESS);
		LFGHandler.saveData();
	}

	static void doCommandSetLFGChannel(MessageCreateEvent event) {
		if (!DiscordHandler.isAdmin(event.getMember().get())) {
			DiscordHandler.reply(event, Messages.LACKS_PERMISSION_GENERIC);
			return;
		}
		LFGHandler.lfgChannel = event.getMessage().getChannelId();
		LFGHandler.saveData();
		DiscordHandler.replyPrivately(event, Messages.LFG_SET_CHANNEL_SUCCESS);
	}

	static void doFunnyGretchenResponse(MessageCreateEvent event) {
		DiscordHandler.reply(event, Gretchen.createString().toUpperCase());
	}

	private static void doPostScheduleChronological(MessageCreateEvent event) {
		// Determine Timezone
		String timezone = SchedulingHandler.defaultTimeZone;
		String message = event.getMessage().getContent();
		String[] words = message.split(" ");
		if (words.length == 2) {
			String tz = words[1].toUpperCase();
			if (SchedulingHandler.isValidTimeZone(tz)) {
				timezone = tz;
			} else {
				DiscordHandler.reply(event, "Invalid Time Zone");
				return;
			}

		} else if (words.length > 2) {
			DiscordHandler.reply(event, "Incorrect Format");
			return;
		}

		// Get The Posts
		List<SchedulePost> posts = SchedulingHandler.getSchedulingPostList();
		if (posts == null) {
			DiscordHandler.reply(event, "ERROR: Scheduling Channel Not Set");
			return;
		}
		// Sort the Posts
		posts.sort(Comparator.comparing(o -> o.getDate()));

		// Start creating the post
		Iterator<SchedulePost> i = posts.iterator();
		List<String> messages = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		System.out.println("Producing String");
		if (timezone.equals(SchedulingHandler.defaultTimeZone)) {
			sb.append(Messages.SCHEDULE_TIMEZONE_INFO + "\n");
		} else {
			sb.append("Timezone: " + timezone + "\n");
		}
		Guild g = event.getGuild().onErrorResume(err -> Mono.empty()).block();
		while (i.hasNext()) {
			StringBuilder iString = new StringBuilder();
			SchedulePost sp = i.next();
			if (sp.getDate().compareTo(new Date()) < 0) {
				continue;
			}
			String gmTitle;
			if (g != null) {
				Member m = g.getMemberById(sp.getGm()).onErrorResume(err -> Mono.empty()).block();
				if (m != null) {
					gmTitle = m.getDisplayName();
				} else {
					gmTitle = DiscordHandler.client.getUserById(sp.getGm()).getData().block().username();
				}
			} else {
				gmTitle = DiscordHandler.client.getUserById(sp.getGm()).getData().block().username();
			}
			System.out.println("Adding Post: " + sp.getTitle());
			iString.append("**");
			iString.append(SchedulingHandler.interpritPostDate(sp, timezone));
			iString.append("**: *");
			if (sp.getTitle().length() > SchedulingHandler.SESSION_TITLE_LENGTH) {
				iString.append(sp.getTitle().substring(0, SchedulingHandler.SESSION_TITLE_LENGTH) + "...");
			} else {
				iString.append(sp.getTitle());
			}
			iString.append("*. **GM:** ");
			iString.append(gmTitle);
			if (sb.length() + iString.length() >= DiscordHandler.DISCORD_CHAR_LIMIT) {
				messages.add(sb.toString());
				sb = iString;
			} else {
				sb.append(iString);
			}
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
		if (sb.length() > 0) {
			messages.add(sb.toString());
		}
		System.out.println("Finished!");
		for (int x = 0; x < messages.size(); x++) {
			DiscordHandler.reply(event, messages.get(x));
		}
	}

	static void doSearchCharacter(MessageCreateEvent event) {
		String message = event.getMessage().getContent();
		String[] words = message.split(" ");
		if (words.length < 2) {
			DiscordHandler.reply(event, Messages.REGISTRY_CHARACTERSEARCHBYNAME_FAIL_NO_CHARACTER_MENTIONED);
			return;
		}
		String characterName = message.substring(words[0].length() + 1);
		System.out.println(characterName);
		if (RegistryHandler.isRegistryOnline()) {
			try {
				String characterSheet = GoogleSheets.searchForCharacter(characterName);
				DiscordHandler.reply(event, characterSheet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				DiscordHandler.reply(event, Messages.REGISTRY_FAILURE_BACKEND);
				return;
			}
		} else {
			DiscordHandler.reply(event, Messages.REGISTRY_FAILURE_BACKEND);
		}
	}

	static void executeCommandHandling() {
		DiscordHandler.gateway.on(MessageCreateEvent.class)
				// subscribe is like block, in that it will *request* for action
				// to be done, but instead of blocking the thread, waiting for it
				// to finish, it will just execute the results asynchronously.
				.subscribe(event -> {
					if (event.getMessage().getAuthor().get().isBot())
						return;
					final String content = event.getMessage().getContent();
					System.out.println(content);
					for (final Map.Entry<String, Command> entry : commands.entrySet()) {

						if (content.startsWith(commandPrefix + entry.getKey())) {
							entry.getValue().execute(event);
							break;
						}
					}
				});
	}

}
