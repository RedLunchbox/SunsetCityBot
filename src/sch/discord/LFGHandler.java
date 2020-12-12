package sch.discord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;

public class LFGHandler {

	static final long GM_NULL_VALUE = 454545;
	static final long CHANNEL_NULL_VALUE = 3333333;

	static String setLFGEmojiCommand;

	static Set<Integer> lfgEmojis = new HashSet<>();
	static Snowflake lfgChannel;
	static Snowflake gmRole;

	final static String lfgdatapath = "data/lfgdata.dat";

	static void executeLFGMonitoring() {
		loadData();
		DiscordHandler.gateway.on(ReactionAddEvent.class).subscribe(event -> {
			onEmojiReaction(event);

		});
	}

	static void onEmojiReaction(ReactionAddEvent event) {
		if (event.getMessage().block().getContent().equals(CommandHandler.commandPrefix + setLFGEmojiCommand)) {
			if (!DiscordHandler.isAdmin(event.getMember().get())) {
				DiscordHandler.replyPrivately(event.getMember().get(), Messages.LACKS_PERMISSION_EMOJI);
				return;
			}
			ReactionEmoji emoji = event.getEmoji();
			lfgEmojis.add(emoji.hashCode());
			saveData();
			System.out.println("Added Emoji: " + emoji.hashCode());
			return;
		}

		if (gmRole == null || lfgChannel == null) {
			System.out.println("Null value found in GM Role or LFG Channel");
			return;
		}
		if (!event.getChannelId().equals(lfgChannel)) {
			return;
		}

		if (!lfgEmojis.contains(event.getEmoji().hashCode())) {
			return;
		}
		if (event.getMember().get().getRoleIds().contains(gmRole)) {
			System.out.println("User is a GM");
			return;
		}
		event.getMessage().block().removeReaction(event.getEmoji(), event.getUserId()).block();
		DiscordHandler.replyPrivately(event.getMember().get(), Messages.LFG_NOT_GM_MESSAGE);
	}

	static void loadData() {
		System.out.println("Loading LFG Data");
		try {
			ObjectInputStream ein = new ObjectInputStream(new FileInputStream(lfgdatapath));
			long input;
			//
			// First GMRole, then LFG Channel, then ReactionEmoji(s)
			//
			input = ein.readLong();
			System.out.println("GM Value Snowflake: " + input);
			if (input != GM_NULL_VALUE) {
				gmRole = Snowflake.of(input);
			} else {
				System.out.println("Loading GM Role found null value");
			}
			input = ein.readLong();
			System.out.println("LFG Channel Snowflake: " + input);
			if (input != CHANNEL_NULL_VALUE) {
				lfgChannel = Snowflake.of(input);
			} else {
				System.out.println("Loading LFG Channel found null value");
			}

			// Now do emojis
			int limit = ein.readInt();
			System.out.println("Detected " + limit + " emoji(s)");
			for (int x = 0; x < limit; x++) {
				lfgEmojis.add(ein.readInt());
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

	static void saveData() {
		System.out.println("Saving LFG Data");
		try {
			ObjectOutputStream eout = new ObjectOutputStream(new FileOutputStream(lfgdatapath));
			if (gmRole != null) {
				eout.writeLong(gmRole.asLong());
			} else {
				eout.writeLong(GM_NULL_VALUE);
			}
			if (lfgChannel != null) {
				eout.writeLong(lfgChannel.asLong());
			} else {
				eout.writeLong(CHANNEL_NULL_VALUE);
			}
			// Save Emojis
			eout.writeInt(lfgEmojis.size());
			Iterator<Integer> i = lfgEmojis.iterator();
			while (i.hasNext()) {
				eout.writeInt(i.next());
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

}
