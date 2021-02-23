package gretchen;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Gretchen {
	
	private static Library lib = new Library();
	
	private static String LIBRARY_PATH = "data/gretchendata";
	
	public static void LoadMemoryFile() throws FileNotFoundException
	{
		File memoryFile = new File(LIBRARY_PATH);
		Scanner in;
		in = new Scanner(memoryFile);
		while (in.hasNextLine()) {
			lib.addLine(in.nextLine().toLowerCase());
		}
		in.close();
	}
	
	
	public static String createString() {
		StringBuilder sb = new StringBuilder();
		String prefix = lib.randomStarterPrefix();
		String suffix = null;
		sb.append(prefix);
		sb.append(" ");
		for (int x = 0; x < 20; x++) {
			suffix = lib.randomSuffix(prefix);
			if (suffix == null)
				break;
			sb.append(suffix);
			sb.append(" ");
			if (x > 5) {
				if (lib.getSuffixes(prefix).contains(Library.END_MARK)) {
					break;
				}
			}
			prefix = suffix;
		}
		return sb.toString();
	}

}
