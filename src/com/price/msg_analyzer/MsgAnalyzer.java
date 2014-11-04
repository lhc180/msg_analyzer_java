package com.price.msg_analyzer;

public class MsgAnalyzer 
{
	private static ErrorWriter error_writer = ErrorWriter.get_instance();
	
	public static void main(String args[])
	{
		System.out.println("MsgAnalyzer !!!");
		error_writer.write_debug_msg("This is a DEBUG message");
		error_writer.write_info_msg("This is a INFO message");
		error_writer.write_warn_msg("This is a WARN message");
		error_writer.write_error_msg("This is a ERROR message");
	}
}
