package sch.discord;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiSearcher {
	
	public static boolean wikiServiceRunning=true;
	
	
	public static String searchFor(String in)
	{
		String out=null;
		Document document;
		try {
			document = Jsoup
			        .connect("http://sch.wikidot.com/search:site/q/" + in)
			        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0")     
			        .get();
			 Elements links = document.select("div.search-results").select("a[href]");
			 for (Element link : links) {
				 	
		            String temp = link.attr("href");
		            return temp;

		        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out;
	}

}
