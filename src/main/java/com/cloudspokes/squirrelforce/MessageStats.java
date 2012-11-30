package com.cloudspokes.squirrelforce;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageStats {
	private static Map<String, Integer> stats = new LinkedHashMap<String, Integer>();
	private static String[] langs;

	public MessageStats(String[] langs) {
		this.langs = langs;
		
		//Initialize the stats
		stats.put(Constants.MAIN_RECEIVER_KEY, 0);
		stats.put(Constants.MAIN_SENDER_KEY, 0);
		
		for (String lang : langs) {
			stats.put(Constants.LANG_RECEIVER_KEY_PREFIX + lang, 0);
		}
	}
	
	//Synchronized methods to update counts
	public static synchronized void receivedInMainQ() {
		stats.put(Constants.MAIN_RECEIVER_KEY, stats.get(Constants.MAIN_RECEIVER_KEY) + 1);
	}
	public static synchronized void sentInMainQ() {
		stats.put(Constants.MAIN_SENDER_KEY, stats.get(Constants.MAIN_SENDER_KEY) + 1);
	}
	public static synchronized void receivedLangQ(String lang) {
		stats.put(Constants.LANG_RECEIVER_KEY_PREFIX + lang, stats.get(Constants.LANG_RECEIVER_KEY_PREFIX + lang) + 1);
	}
	
	//Return current data for UI
	public static Map<String, Integer> getStats() {
		return stats;
	}

	public static String[] getLangs() {
		return langs;
	}
	
}
