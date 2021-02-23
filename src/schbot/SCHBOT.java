package schbot;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import sch.discord.DiscordHandler;

public class SCHBOT {

	/*
	 * TO DO: Implement Proper Exception Handling Un-Hard-Code the AutoPrune
	 * feature.
	 */

	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, ClassNotFoundException {
		// TODO Auto-generated method stub
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
