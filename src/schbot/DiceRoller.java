package schbot;

public class DiceRoller {
	
	public static final String[] fateResults = {"-","0","+"};
	
	public static String rollFateDice(int number, int modifier)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int outputNum=modifier;
		for (int x=0;x<number;x++)
		{
			int result = diceFateRoll();
			outputNum+= result-1;
			sb.append(fateResults[result]);
			
		}
		sb.append(") Result: ");
		sb.append(outputNum);
		return sb.toString();
	}
	
	private static int diceFateRoll()
	{
		return (int)(Math.random()*3);
	}

}
