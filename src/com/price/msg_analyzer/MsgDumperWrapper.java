package com.price.msg_analyzer;

import com.price.msg_dumper.*;

public class MsgDumperWrapper 
{
	private static MsgDumperWrapper instance = null;
	static MsgDumper msg_dumper = null;

	public static MsgDumperWrapper get_instance()
	{
		if (instance == null)
		{
			instance = new MsgDumperWrapper();
			instance.init();
		}
		return instance;
	}

	private MsgDumperWrapper()
	{
		
	}

	public Object clone() throws CloneNotSupportedException 
	{
		throw new CloneNotSupportedException();
	}

	private void init()
	{
		short ret = MsgDumperCmnDef.MSG_DUMPER_SUCCESS;
		short severity = MsgDumperCmnDef.MSG_DUMPER_SEVIRITY_ERROR;
		short facility = MsgDumperCmnDef.MSG_DUMPER_FACILITY_LOG;
		System.out.printf("Error Writer API version: (%s)\n", MsgDumper.get_version());
// Set severity
		System.out.printf("Error Writer Set severity to :%d\n", severity);
		ret = MsgDumper.set_severity(severity);
		if (MsgDumperCmnDef.CheckMsgDumperFailure(ret))
		{
			System.out.printf("Error Writer set_severity() fails, due to %d\n", ret);
			System.exit(1);
		}
// Set facility
		System.out.printf("Error Writer Set facility to :%d\n", facility);
		ret = MsgDumper.set_facility(facility);
		if (MsgDumperCmnDef.CheckMsgDumperFailure(ret))
		{
			System.out.printf("Error Writer set_facility() fails, due to %d\n", ret);
			System.exit(1);
		}
// Initialize the library
		System.out.printf("Error Writer Initialize the library\n");
		ret = MsgDumper.initialize(".");
		if (MsgDumperCmnDef.CheckMsgDumperFailure(ret))
		{
			System.out.printf("Error Writer initialize() fails, due to %d\n", ret);
			System.exit(1);
		}
	}

// Write the Error message
	public void write_error_msg(String msg)
	{
		MsgDumper.write_msg(MsgDumperCmnDef.MSG_DUMPER_SEVIRITY_ERROR, msg);
	}

// Write the Warning message
	public void write_warn_msg(String msg)
	{
		MsgDumper.write_msg(MsgDumperCmnDef.MSG_DUMPER_SEVIRITY_WARN, msg);
	}

// Write the Info message
	public void write_info_msg(String msg)
	{
		MsgDumper.write_msg(MsgDumperCmnDef.MSG_DUMPER_SEVIRITY_INFO, msg);
	}

// Write the Debug message
	public void write_debug_msg(String msg)
	{
		MsgDumper.write_msg(MsgDumperCmnDef.MSG_DUMPER_SEVIRITY_DEBUG, msg);
	}
}
