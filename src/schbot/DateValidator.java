package schbot;

import java.util.Date;
import java.util.Set;

public class DateValidator implements Comparable<DateValidator>{
	
	private Date date;
	private boolean explicitTime;
	private boolean explicitDate;
	
	public DateValidator(Date date, Set<String> syntaxTreeSet)
	{
		setDate(date);
		setExplicitTime(syntaxTreeSet.contains("EXPLICIT_TIME"));
		setExplicitDate(syntaxTreeSet.contains("EXPLICIT_DATE"));
	}
	

	@Override
	public int compareTo(DateValidator o) {
		if (isExplicitDate() && !o.isExplicitDate())
		{
			return -1;
		}
		if (!isExplicitDate() && o.isExplicitDate())
		{
			return 1;
		}
		return 0;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}


	public boolean isExplicitDate() {
		return explicitDate;
	}


	public void setExplicitDate(boolean explicitDate) {
		this.explicitDate = explicitDate;
	}


	public boolean isExplicitTime() {
		return explicitTime;
	}


	public void setExplicitTime(boolean explicitTime) {
		this.explicitTime = explicitTime;
	}



}
