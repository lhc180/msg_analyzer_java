package com.price.msg_analyzer;

import java.util.regex.*;
import java.io.*;


public class MsgAnalyzerCmnDef
{
	private static MsgDumperWrapper msg_dumper = MsgDumperWrapper.get_instance();

// Does NOT work !!!
//	public static final String DUMP_RST = "\u001B[0m";
//	public static final String DUMP_BLK = "\u001B[30m";
//	public static final String DUMP_RED = "\u001B[31m";
//	public static final String DUMP_GRN = "\u001B[32m";
//	public static final String DUMP_YEL = "\u001B[33m";
//	public static final String DUMP_BLU = "\u001B[34m";
//	public static final String DUMP_PUR = "\u001B[35m";
//	public static final String DUMP_CYN = "\u001B[36m";
//	public static final String DUMP_WHT = "\u001B[37m";

	public static final byte SHOW_DEVICE_NONE = 0x0;
	public static final byte SHOW_DEVICE_CONSOLE = 0x1;
	public static final byte SHOW_DEVICE_SYSLOG = 0x2;
	private static boolean checkShowDeviceEnable(byte value, byte flag){return ((value & flag) != 0 ? true : false);}

	static public String DEF_DEVICE_FILE = "/dev/ttyUSB1";
	static public int DEF_BAUD_RATE = 115200;
	static public String CONF_FOLDER_NAME = "conf";
	static public String CONF_FILE_NAME = "serial_port_param.conf";
	static private boolean SHOW_CONSOLE = true;
	static private boolean SHOW_CONSOLE_ERROR = SHOW_CONSOLE && true;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Enumeration
	enum SeverityType {SEVERITY_ERROR, SEVERITY_WARN, SEVERITY_INFO, SEVERITY_DEBUG, SEVERITY_NONE};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Return values
	public static final short ANALYZER_SUCCESS = 0;

	public static final short ANALYZER_FAILURE_UNKNOWN = 1;
	public static final short ANALYZER_FAILURE_INVALID_ARGUMENT = 2;
	public static final short ANALYZER_FAILURE_INVALID_POINTER = 3;
	public static final short ANALYZER_FAILURE_INSUFFICIENT_MEMORY = 4;
	public static final short ANALYZER_FAILURE_HANDLE_SERIAL = 5;
	public static final short ANALYZER_FAILURE_NOT_FOUND = 6;
	public static final short ANALYZER_FAILURE_INCORRECT_CONFIG = 7;
	public static final short ANALYZER_FAILURE_HANDLE_THREAD = 8;
	public static final short ANALYZER_FAILURE_IGNORE_DATA = 9;
	public static final short ANALYZER_FAILURE_WAIT_SCG_COMMAND = 10;
	public static final short ANALYZER_FAILURE_INCORRECT_SERIAL_DATA = 11;

	public static boolean CheckSuccess(short x) 
	{
		return (x == ANALYZER_SUCCESS ? true : false);
	}
	public static boolean CheckFailure(short x) 
	{
		return !CheckSuccess(x);
	}
	public static boolean CheckIgnoreData(short x) 
	{
		return (x == ANALYZER_FAILURE_IGNORE_DATA ? true : false);
	}
	public static boolean CheckWaitSCGCommand(short x) 
	{
		return (x == ANALYZER_FAILURE_WAIT_SCG_COMMAND ? true : false);
	}
	
	public static int MIN_SERAIL_DATA_TITLE_LENGTH = 20;
	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Functions	
	public static void WriteDebugFormatSyslog(String format, Object... arguments)
	{
		String msg = String.format(format, arguments);
		WriteDebugSyslog(msg);
	}

	public static void WriteInfoFormatSyslog(String format, Object... arguments)
	{
		String msg = String.format(format, arguments);
		WriteInfoSyslog(msg);
	}

	public static void WriteWarnFormatSyslog(String format, Object... arguments)
	{	
		String msg = String.format(format, arguments);
		WriteWarnSyslog(msg);
	}

	public static void WriteErrorFormatSyslog(String format, Object... arguments)
	{
		String msg = String.format(format, arguments);
		WriteErrorSyslog(msg);
	}

	public static void WriteDebugSyslog(String msg)
	{
		if (SHOW_CONSOLE)
			System.out.println(msg);
		msg_dumper.write_debug_msg(msg);
	}

	public static void WriteInfoSyslog(String msg)
	{
		if (SHOW_CONSOLE)
			System.out.println(msg);
		msg_dumper.write_info_msg(msg);
	}

	public static void WriteWarnSyslog(String msg)
	{
		if (SHOW_CONSOLE)
			System.out.println(msg);
		msg_dumper.write_warn_msg(msg);
	}

	public static void WriteErrorSyslog(String msg)
	{
		if (SHOW_CONSOLE_ERROR)
			System.err.println(msg);
		msg_dumper.write_error_msg(msg);
	}

//	public static String get_severity_color(final String severity)
//	{
//		String[] severity_str = {"Error", "Warn", "Info", "Debug"};
//		String[] severity_color = {DUMP_RED, DUMP_YEL, DUMP_GRN, DUMP_WHT};
//
//		SeverityType severity_type = SeverityType.SEVERITY_NONE;
//		for(int index = severity_str.length - 1 ; index >= 0 ; index--)
//		{
//			if(severity.indexOf(severity_str[index]) >= 0)
//			{
//				severity_type = SeverityType.values()[index];
//				break;
//			}
//		}
//
//		if (severity_type == SeverityType.SEVERITY_NONE)
//		{
//			WriteErrorFormatSyslog("Unknown Severity[%s]", severity);
//			return null;
//		}
//
//		return severity_color[severity_type.ordinal()];
//	}

	public static short parse_serial_parameter(final String cur_serial_data, boolean highlight, byte show_device_flags)
	{
		if (show_device_flags == SHOW_DEVICE_NONE)
			return ANALYZER_SUCCESS;

		String title = null;
		String content = null;
		String date = null;
		String time = null;
		String number = null;
		String severity = null;
//		String severity_color = null;
		String tmp_serial_data = cur_serial_data;

//			printf("%s [%d]\n", serial_data, serial_data_size);
		short ret = ANALYZER_SUCCESS;
		int pos = tmp_serial_data.indexOf(": ");
		if (pos < 0)
		{
			WriteDebugFormatSyslog("Ignore incorrect data format(1): %s", tmp_serial_data);
			ret = ANALYZER_FAILURE_IGNORE_DATA;
		}
		else
		{
			if (pos < MIN_SERAIL_DATA_TITLE_LENGTH)
			{
				WriteDebugFormatSyslog("Ignore incorrect data format(2): %s", tmp_serial_data);
				ret = ANALYZER_FAILURE_IGNORE_DATA;
			}
			else
			{
// Copy the title
				title = tmp_serial_data.substring(0, pos);
// Parse the title
				Pattern datePattern = Pattern.compile("([\\d/]{5}) ([\\d:]{8}) ([\\d]{3,5}) ([\\w]{4,5})");
				Matcher dateMatcher = datePattern.matcher(title);
				if (dateMatcher.find())
				{
					date = dateMatcher.group(1);
					time = dateMatcher.group(2);
					number = dateMatcher.group(3);
					severity =  dateMatcher.group(4);
//					WriteDebugFormatSyslog("Date: %s, Time: %s, Number: %s, Severity: %s", date, time, number, severity);
	// Copy the content
					content = tmp_serial_data.substring(pos + 2);
//					WriteDebugFormatSyslog("Content: %s", content);
//					severity_color = get_severity_color(severity);
//					if (severity_color == null)
//						ret = ANALYZER_FAILURE_INCORRECT_SERIAL_DATA;
				}
				else
				{
					WriteDebugFormatSyslog("Ignore incorrect data format(3): %s", title);
					ret = ANALYZER_FAILURE_IGNORE_DATA;
				}
			}
		}

		if (CheckSuccess(ret))
		{
			String res = String.format("Date: %s, Time: %s, Number: %s, Severity: %s, Content: %s", date, time, number, severity, content);
			if (checkShowDeviceEnable(show_device_flags, SHOW_DEVICE_CONSOLE))
				System.out.println(res);
//				System.out.printf("%s%s\n",(highlight ? DUMP_CYN : severity_color), res);
			if (checkShowDeviceEnable(show_device_flags, SHOW_DEVICE_SYSLOG))
				WriteDebugSyslog(res);
		}

		return ret;
	}

	public static String get_cur_path()
	{
		String cur_path = null;
		try 
		{
			File cur_dir = new File (".");
			cur_path = cur_dir.getCanonicalPath();
		}
		catch(Exception e)
		{
			String msg = String.format("Fail to get the current path: %s", e.toString());
			WriteErrorSyslog(msg);
		}
		return cur_path;
	}
}
