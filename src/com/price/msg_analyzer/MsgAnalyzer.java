package com.price.msg_analyzer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgAnalyzer 
{
//	private static MsgDumperWrapper error_writer = MsgDumperWrapper.get_instance();
	
	public static void main(String args[])
	{
//		System.out.println("MsgAnalyzer !!!");
//		error_writer.write_debug_msg("This is a DEBUG message");
//		error_writer.write_info_msg("This is a INFO message");
//		error_writer.write_warn_msg("This is a WARN message");
//		error_writer.write_error_msg("This is a ERROR message");
		String my_str = "11/10 05:56:18 755 Debug: httpTimestampHandler 3310 Received Timestamp: Mon, 10 Nov 2014 05:56:19 GMT";
		int pos = my_str.indexOf(": ");
		String title = my_str.substring(0, pos);
//		Pattern datePattern = Pattern.compile("(\\w{5}) (\\w{8}) (\\w{3}) (\\w{5})");
		Pattern datePattern = Pattern.compile("([\\d/]{5}) ([\\d:]{8}) ([\\d]{3}) ([\\w]{4,5})");
		Matcher dateMatcher = datePattern.matcher(title);
		if (dateMatcher.find()) 
		{
			System.out.println("Date: " + dateMatcher.group(1));
			System.out.println("Time: " + dateMatcher.group(2));
			System.out.println("Number: " + dateMatcher.group(3));
			System.out.println("Severity: " + dateMatcher.group(4));
		}		
	}
}
