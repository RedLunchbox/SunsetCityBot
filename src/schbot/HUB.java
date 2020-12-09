package schbot;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import sch.discord.DiscordHandler;

public class HUB extends JFrame implements WindowListener, UIContainer {

	private static final long serialVersionUID = 1L;
	public static final String UI_PROGRAM_NAME = "SCHBOT";
	private DiscordHandler discordHandler;

	public HUB() {
		addWindowListener(this);
		this.setTitle(UI_PROGRAM_NAME);
		this.setSize(320, 240);
		this.setResizable(false);
		DiscordHandler.UIContainer=this;
	}

	@Override
	public void displayError(String error) {
		JOptionPane.showMessageDialog(this, error, UI_PROGRAM_NAME, JOptionPane.WARNING_MESSAGE);

	}

	public DiscordHandler getDiscordHandler() {
		return discordHandler;
	}

	@Override
	public void report(String report) {
		// TODO Auto-generated method stub

	}

	public void setDiscordHandler(DiscordHandler discordHandler) {
		this.discordHandler = discordHandler;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		if (discordHandler != null) {
			discordHandler.logout();
		}
		System.exit(0);

	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		if (discordHandler != null) {
			discordHandler.logout();
		}
		System.exit(0);

	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public String prompt(String question) {
		// TODO Auto-generated method stub
		return null;
	}
}
