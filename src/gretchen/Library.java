package gretchen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Library implements Serializable {

	private static final long serialVersionUID = 1L;
	Map<String, List<String>> strings = new HashMap<>();
	List<String> prefixes = new ArrayList<String>();
	List<String> starterPrefixes = new ArrayList<>();
	public static final String END_MARK = "<.lnh$û{¢";

	public Library() {

	}

	public void add(String word, String suffix) {
		if (strings.containsKey(word)) {
			strings.get(word).add(suffix);
		} else {
			strings.put(word, new ArrayList<>());
			prefixes.add(word);
			strings.get(word).add(suffix);
		}
	}

	public List<String> getSuffixes(String prefix) {
		return strings.get(prefix);
	}

	public boolean addLine(String line) {
		if (line.length() == 0) {
			return false;
		}
		List<String> words = Arrays.asList(line.split(" "));
		if (words.size() < 2)
			return false;
		starterPrefixes.add(words.get(0));
		for (int x = 0; x < words.size() - 1; x++) {
			String prefix = words.get(x);
			String suffix = words.get(x + 1);
			add(prefix, suffix);
			if (x + 1 == words.size()) {
				add(suffix, END_MARK);
			}
			if (prefix.contains(".")) {
				starterPrefixes.add(suffix);
			}
		}
		return true;
	}

	public String randomPrefix() {
		int i = (int) (Math.random() * prefixes.size());
		return prefixes.get(i);

	}
	
	public String randomStarterPrefix() {
		int i = (int) (Math.random() * starterPrefixes.size());
		return starterPrefixes.get(i);

	}

	public String randomSuffix(String prefix) {
		if (!strings.containsKey(prefix))
			return null;
		List<String> suffixes = strings.get(prefix);

		int i = (int) (Math.random() * suffixes.size());
		return suffixes.get(i);

	}

}
