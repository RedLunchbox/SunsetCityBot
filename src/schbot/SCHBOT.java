package schbot;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import sch.discord.DiscordHandler;

public class SCHBOT {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			UIManager.setLookAndFeel(
			        UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HUB hub = new HUB();
		hub.setDiscordHandler(new DiscordHandler());
		hub.setVisible(true);
		DiscordHandler.init();

	}

}
