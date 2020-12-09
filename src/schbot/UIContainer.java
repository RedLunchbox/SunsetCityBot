package schbot;

public interface UIContainer {
	
	public abstract void displayError(String error);
	
	public abstract void report(String report);
	
	public abstract String prompt(String question);

}
