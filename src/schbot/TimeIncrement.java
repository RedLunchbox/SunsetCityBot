package schbot;

public class TimeIncrement {
	
	public int getValue() {
		return value;
	}


	public ValidTimeUnits getIncrement() {
		return increment;
	}

	public final int value;
	public final ValidTimeUnits increment;
	
	public TimeIncrement(int value, ValidTimeUnits increment)
	{
		this.value=value;
		this.increment=increment;
	}
	
	public String toMessageString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(value);
		sb.append(" ");
		sb.append(increment.toString().toLowerCase());
		if (value > 1) {
			sb.append("s");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((increment == null) ? 0 : increment.hashCode());
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeIncrement other = (TimeIncrement) obj;
		if (increment != other.increment)
			return false;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value+" "+increment;
	}

}
