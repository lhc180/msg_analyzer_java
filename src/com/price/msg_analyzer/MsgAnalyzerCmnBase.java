package com.price.msg_analyzer;

public class MsgAnalyzerCmnBase 
{
	protected static String __CLASSNAME__()
	{
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		return ste.getClassName();
	}
	protected static String __FILE__()
	{
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		return ste.getFileName();
	}
	protected static String __FUNC__()
	{
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		return ste.getMethodName();
	}
	protected static int __LINE__()
	{
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		return ste.getLineNumber();
	}
}
