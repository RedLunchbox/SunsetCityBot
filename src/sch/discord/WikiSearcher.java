package sch.discord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiSearcher {
	
	private static Pattern patternDomainName;
	private static Matcher matcher;
	  private static final String DOMAIN_NAME_PATTERN 
	    = "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}";
	  static {
	    patternDomainName = Pattern.compile(DOMAIN_NAME_PATTERN);
	  }
	
	  public static String searchFor(String in)
	  {
		 return searchFor(in, null);
	  }
	
	public static String searchFor(String in, String[] regrex)
	{
		String out=null;
		Document document;
		try {
			document = Jsoup
			        .connect("http://sch.wikidot.com/search:site/q/" + in)
			        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0")     
			        .get();
			 Elements links = document.select("div.search-results").select("a[href]");
					 // > a[href]");
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
	
public static String getDomainName(String url){
        
	    String domainName=null;
		try {
			domainName = URLDecoder.decode(url.substring(url.indexOf('=') + 
				    1, url.indexOf('&')), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return domainName;
	        
	  }

}
