package com.price.msg_analyzer;

import java.lang.Thread;


public class MsgAnalyzer 
{	
	public static void main(String args[])
	{
		System.out.println("Start the read the data from serial port......");

		SerialAnalyzer serial_analyzer = new SerialAnalyzer();
		serial_analyzer.initialize();

		try
		{
			Thread.sleep(1000000);
		}
		catch(InterruptedException e){}

		serial_analyzer.deinitialize();
	}
}
