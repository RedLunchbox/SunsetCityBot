package sch.discord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import org.antlr.runtime.tree.Tree;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserWithMemberData;
import discord4j.rest.entity.RestChannel;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import reactor.core.publisher.Flux;
import schbot.DateValidator;
import schbot.SchedulePost;
import schbot.ScheduleTimer;
import schbot.TimeIncrement;
import schbot.ValidTimeUnits;

public class SchedulingHandler {

	final static String SCHEDULING_DATA_PATH = "data/schedulingdata.dat";
	private static final String SCHEDULING_REMINDER_PATH = "data/schedulingreminders.dat";
	final static int CURRENT_SCHEUDLING_DATA_IDENTIFIER = 1;

	final static String[] daysOfWeek = { "What", "Sun", "Mon", "Tue", "Wed", "Thurs", "Fri", "Sat" };
	final static String[] monthsOfYear = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
			"Dec" };
	final static String[] AM_PM = { "AM", "PM" };

	@SuppressWarnings("unchecked")
	private static final Set<String> TIMEZONES = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));

	private static final Map<Snowflake, Set<TimeIncrement>> peopleToRemind = new HashMap<>();

	static String defaultTimeZone;
	static List<String> excludeFromPost = new ArrayList<>();
	static final String PROPERTIES_PATH = "cfg/scheduling.txt";

	static List<SchedulePost> schedulingPosts;
	static Set<ScheduleTimer> timers = new HashSet<>();

	static final int SESSION_TITLE_LENGTH = 100;

	public static Snowflake schedulingChannelSnowflake;
	public static int maximum_reminders;

	public static long scheduling_cutoff_post = 0;

	static void addReminder(int timeBeforeReminder, ValidTimeUnits increment, Snowflake UserID) {
		if (peopleToRemind.containsKey(UserID)) {
			peopleToRemind.get(UserID).add(new TimeIncrement(timeBeforeReminder, increment));
		} else {
			Set<TimeIncrement> s = new HashSet<>();
			s.add(new TimeIncrement(timeBeforeReminder, increment));
			peopleToRemind.put(UserID, s);
		}
		saveSchedulingReminderList();
		setUpTimers();
	}

	// Can probably be cleaned up unless I find a case where groups is greater than
	// one.
	public static SchedulePost checkDateGroups(SchedulePost post, List<DateGroup> groups) {
		List<DateValidator> dates = new LinkedList<>();
		boolean sortNeeded = false;
		for (int x = 0; x < groups.size(); x++) {
			DateGroup dg = groups.get(x);
			Set<String> parseTreeSet = loadTreeIntoSet(new HashSet<>(), dg.getSyntaxTree());
			DateValidator dv = new DateValidator(dg.getDates().get(0), parseTreeSet);
			if (x == 0 && dv.isExplicitDate() && groups.size() > 1) {
				System.out.println("Sort Needed");
				sortNeeded = true;
			}
			dates.add(dv);
			post.setDate(dg.getDates().get(0));
		}
		if (sortNeeded) {
			Collections.sort(dates);
		}
		DateValidator finalDate = dates.get(0);
		post.setValid(finalDate.isExplicitDate());
		if (!finalDate.isExplicitTime()) {
			post.setHideTime(true);
		}
		post.setDate(finalDate.getDate());

		return post;
	}

	public static void clearReminders(Snowflake UserID) {
		peopleToRemind.remove(UserID);
		timers.removeIf(t -> t.checkUserAndCancel(UserID));
		saveSchedulingReminderList();

	}

	public static void removeReminder(Snowflake UserID, TimeIncrement ti) {
		peopleToRemind.get(UserID).remove(ti);
		if (peopleToRemind.get(UserID).size() == 0) {
			peopleToRemind.remove(UserID);
		}
		saveSchedulingReminderList();
		removeTimersByTimeIncrement(UserID, ti);
	}

	static void createTimers(Snowflake UserID, SchedulePost sp) {
		Set<TimeIncrement> s = peopleToRemind.get(UserID);
		Iterator<TimeIncrement> i = s.iterator();
		while (i.hasNext()) {
			TimeIncrement ti = i.next();
			ScheduleTimer t = ScheduleTimer.construct(ti, sp, UserID);
			if (timers.contains(t)) {
				System.out.println("Timer Already Here");
				continue;
			}
			if (t.isAfterNow()) {
				t.start();
				timers.add(t);
			}
		}
	}

	public static Map<Snowflake, Set<TimeIncrement>> getPeopletoremind() {
		return peopleToRemind;
	}

	public static List<SchedulePost> getSchedulingPostList() {
		if (schedulingPosts != null) {
			return schedulingPosts;
		}
		if (schedulingChannelSnowflake == null) {
			return null;
		}
		List<SchedulePost> out = new LinkedList<>();
		RestChannel channel = DiscordHandler.client.getChannelById(schedulingChannelSnowflake);
		Flux<MessageData> flux = channel.getMessagesAfter(Snowflake.of(scheduling_cutoff_post));
		List<MessageData> posts = flux.buffer().blockFirst();
		System.out.println("Detected " + posts.size() + " post(s).");
		Iterator<MessageData> i = posts.iterator();
		while (i.hasNext()) {
			SchedulePost sp = parseSchedulePost(i.next());
			if (!isValidPostDate(sp)) {
				continue;
			}
			out.add(sp);
		}
		System.out.println("Moving on!");
		schedulingPosts = out;
		return out;
	}

	@SuppressWarnings("unchecked")
	public static void init() {
		loadSchedulingReminderList();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTIES_PATH));
			defaultTimeZone = properties.getProperty("TIME_ZONE");
			String exclusion = properties.getProperty("EXCLUDE_FROM_POST");
			maximum_reminders = Integer.valueOf(properties.getProperty("MAXIMUM_REMINDERS"));
			schedulingChannelSnowflake = Snowflake.of(properties.getProperty("SCHEDULING_SNOWFLAKE"));
			excludeFromPost = Arrays.asList(exclusion.split(" "));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DiscordHandler.gateway.on(MessageCreateEvent.class).subscribe(event -> {
			if (event.getMessage().getChannelId().equals(schedulingChannelSnowflake)) {
				SchedulePost sp = parseSchedulePost(event.getMessage());
				if (isValidPostDate(sp)) {
					schedulingPosts.add(sp);
					setUpTimersForPost(sp);
				}
			}
		});
		DiscordHandler.gateway.on(MessageDeleteEvent.class).subscribe(event -> {
			if (event.getChannelId().equals(schedulingChannelSnowflake)) {
				System.out.println("Post Deleted");
				Snowflake id = event.getMessageId();
				removeSchedulingPostFromList(id);
				removeTimersByPost(id);
			}
		});
		DiscordHandler.gateway.on(MessageUpdateEvent.class).subscribe(event -> {
			if (event.getChannelId().equals(schedulingChannelSnowflake)) {
				removeTimersByPost(event.getMessageId());
				removeSchedulingPostFromList(event.getMessageId());
				SchedulePost sp = parseSchedulePost(event.getMessage().block());
				if (isValidPostDate(sp)) {
					schedulingPosts.add(sp);
					setUpTimersForPost(sp);
				}
			}
		});
		refreshSchedulingPostList();
		setUpTimers();
	}

	public static String interpritPostDate(SchedulePost post, String timezone) {
		Date date = post.getDate();
		Calendar c = new GregorianCalendar();
		c.setTime(date);
		c.setTimeZone(TimeZone.getTimeZone(timezone));
		StringBuilder sb = new StringBuilder();
		sb.append(daysOfWeek[c.get(Calendar.DAY_OF_WEEK)]);
		sb.append(" ");
		sb.append(monthsOfYear[c.get(Calendar.MONTH)]);
		sb.append(" ");
		sb.append(c.get(Calendar.DAY_OF_MONTH));
		if (c.get(Calendar.YEAR) != +Calendar.getInstance().get(Calendar.YEAR)) {
			sb.append(" ");
			sb.append(c.get(Calendar.YEAR));
		}
		if (!post.isHideTime()) {
			sb.append(", ");
			if (c.get(Calendar.HOUR) == 0) {
				sb.append(12);
			} else
				sb.append(c.get(Calendar.HOUR));
			sb.append(":");
			sb.append(new SimpleDateFormat("mm").format(date));
			sb.append(" ");
			sb.append(AM_PM[c.get(Calendar.AM_PM)]);
		}
		return sb.toString();
	}

	public static boolean isValidPostDate(SchedulePost sp) {
		if (sp.getDate() == null) {
			System.out.println("No Date Detected");
			return false;
		}
		if (sp.isValid() == false) {
			System.out.println("Parsed Invalid date for: " + sp.getTitle());
			return false;
		}
		return true;
	}

	public static boolean isValidTimeZone(String timezone) {
		return TIMEZONES.contains(timezone);
	}

	static void loadSchedulingReminderList() {
		DataInputStream ein;
		try {
			ein = new DataInputStream(new FileInputStream(SCHEDULING_REMINDER_PATH));
			// Read the SchedulingVersion
			ein.readInt();

			int snowflakes = ein.readInt();
			for (int x = 0; x < snowflakes; x++) {
				Snowflake key = Snowflake.of(ein.readLong());
				int reminders = ein.readInt();
				Set<TimeIncrement> value = new HashSet<>();
				for (int y = 0; y < reminders; y++) {
					int itemValue = ein.readInt();
					ValidTimeUnits u = ValidTimeUnits.valueOf(ein.readUTF());
					value.add(new TimeIncrement(itemValue, u));
				}
				peopleToRemind.put(key, value);
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

	public static Set<String> loadTreeIntoSet(Set<String> set, Tree t) {
		set.add(t.getText());
		for (int x = 0; x < t.getChildCount(); x++) {
			loadTreeIntoSet(set, t.getChild(x));
		}
		return set;
	}

	public static SchedulePost parseSchedulePost(Message data) {
		SchedulePost out = parseString(data.getContent());
		out.setPostID(data.getId());
		out.setGm(data.getAuthor().get().getId());
		out.setMentions(new ArrayList<>((data.getUserMentionIds())));
		return out;
	}

	public static SchedulePost parseSchedulePost(MessageData data) {
		SchedulePost out = parseString(data.content());
		out.setPostID(Snowflake.of(data.id()));
		out.setGm(Snowflake.of(data.author().id()));
		List<UserWithMemberData> mentions = data.mentions();
		// Put it as an ArrayList, since after this method it stays the same amount of
		// things
		List<Snowflake> outMentions = new ArrayList<>();
		for (int x = 0; x < mentions.size(); x++) {
			outMentions.add(Snowflake.of(mentions.get(x).id()));
		}
		out.setMentions(outMentions);
		return out;
	}

	public static SchedulePost parseString(String text) {
		SchedulePost post = new SchedulePost();
		Scanner in = null;
		boolean hasTitle = false;
		boolean hasDate = false;
		in = new Scanner(text);
		while (in.hasNextLine()) {
			String line = in.nextLine();
			if (line.length() == 0)
				continue;
			if (hasTitle && hasDate)
				continue;
			if (!hasTitle) {
				String titleLine = line.replace("*", "");
				String[] words = titleLine.split(" ");

				// Crash Prevention, though kind of wonky
				if (words.length > 1) {
					for (int x = 0; x < excludeFromPost.size(); x++) {
						titleLine = titleLine.replace(excludeFromPost.get(x) + " ", "");
					}
					if (titleLine.length() > 0) {
						post.setTitle(titleLine);
						hasTitle = true;
					}
				}
			}
			if (!hasDate && (line.length() < 500)) {
				System.out.println(line);
				Parser parser = new Parser();
				try {
					List<DateGroup> groups = parser.parse(line);
					if (!groups.isEmpty()) {
						post = checkDateGroups(post, groups);
						if (post.isValid()) {								
							hasDate = true;
						}
					}
				} catch (NullPointerException e) {
					System.out.println("Fucked up!");
				}
			}
		}
		in.close();
		return post;
	}

	static void refreshSchedulingPostList() {
		schedulingPosts = null;
		getSchedulingPostList();
	}

	static void removeSchedulingPostFromList(Snowflake id) {
		schedulingPosts.removeIf(sp -> sp.getPostID().equals(id));
	}

	static void removeTimersByPost(Snowflake postId) {
		timers.removeIf(t -> t.checkPostAndCancel(postId));
	}

	static void removeTimersByTimeIncrement(Snowflake authorID, TimeIncrement ti) {
		timers.removeIf(t -> t.checkPostAuthorIncrementAndCancel(authorID, ti));
	}

	static void saveSchedulingReminderList() {
		DataOutputStream eout;
		try {
			eout = new DataOutputStream(new FileOutputStream(SCHEDULING_REMINDER_PATH));
			eout.writeInt(CURRENT_SCHEUDLING_DATA_IDENTIFIER);
			eout.writeInt(peopleToRemind.size());

			for (Entry<Snowflake, Set<TimeIncrement>> entry : peopleToRemind.entrySet()) {
				eout.writeLong(entry.getKey().asLong());
				Set<TimeIncrement> reminders = entry.getValue();
				eout.writeInt(reminders.size());
				Iterator<TimeIncrement> i = reminders.iterator();
				while (i.hasNext()) {
					TimeIncrement t = i.next();
					eout.writeInt(t.getValue());
					eout.writeUTF(t.getIncrement().toString());
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

	static void setUpTimers() {
		System.out.println("Setting Up Timers");
		Iterator<SchedulePost> i = schedulingPosts.iterator();
		while (i.hasNext()) {
			setUpTimersForPost(i.next());
		}

	}

	static void setUpTimersForPost(SchedulePost sp) {
		if (sp.getDate().compareTo(new Date()) < 0) {
			return;
		}
		if (peopleToRemind.containsKey(sp.getGm())) {
			createTimers(sp.getGm(), sp);
		}
		List<Snowflake> mentions = sp.getMentions();
		for (int x = 0; x < mentions.size(); x++) {
			if (peopleToRemind.containsKey(mentions.get(x))) {
				createTimers(mentions.get(x), sp);
			}
		}
	}

	public static void timerOnOff(ScheduleTimer timer) {
		System.out.println("Timer Went Off!");
		// Message Format: SCHEDULE_REMINDER_MESSAGE=Hello! This is a reminder that you
		// are in the session %s in %s.
		DiscordHandler.sendMessage(timer.getUserID(),
				String.format(Messages.SCHEDULE_REMINDER_MESSAGE, timer.getTitle(), timer.formatTimeIncrement()));
		timers.remove(timer);
	}
}
