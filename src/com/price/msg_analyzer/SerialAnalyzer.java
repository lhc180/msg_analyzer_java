package com.price.msg_analyzer;

import java.lang.Thread;
import java.util.concurrent.atomic.*;


public class SerialAnalyzer implements Runnable 
{
	private SerialReceiver serial_receiver = null;
	private Thread t = null;
	private AtomicBoolean exit = new AtomicBoolean(false);
	private String serial_data = null;
	boolean highlight = false;
	
	public SerialAnalyzer()
	{
		serial_receiver = new SerialReceiver(this);
		t = new Thread(this);
	}

	public short initialize()
	{
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;

// Initialize the thread
		t.start();

		try
		{
			Thread.sleep(1);
		}
		catch(InterruptedException e)
		{
			MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Fail to sleep..., due to %s", e.toString());
			return MsgAnalyzerCmnDef.ANALYZER_FAILURE_UNKNOWN;
		}

// Open the serial port
	    MsgAnalyzerCmnDef.WriteDebugSyslog("Initialize the SerialReceiver object\n");
	    ret = serial_receiver.initialize();
	    if (MsgAnalyzerCmnDef.CheckFailure(ret))
	    	return ret;

		return ret;
	}

	public short deinitialize()
	{
		exit.set(true);
// Stop the packet sniffing
		MsgAnalyzerCmnDef.WriteDebugSyslog("De-initialize the SerialReceiver object\n");
		short ret = serial_receiver.deinitialize();
		if (MsgAnalyzerCmnDef.CheckFailure(ret))
			return ret;

// Notify the worker thread of analyzing serial data to die...
//		t.interrupt();
		MsgAnalyzerCmnDef.WriteDebugSyslog("Notify the worker thread of analyzing the serial data it's going to die...");
		synchronized(this)
		{
			notify();
		}
		MsgAnalyzerCmnDef.WriteDebugSyslog("Wait for the worker thread of analyzing serial data's death...");
        try 
        {
//wait till this thread dies
            t.join();
//            System.out.println("The worker thread of receiving serial data is dead");
            MsgAnalyzerCmnDef.WriteDebugSyslog("Wait for the worker thread of analyzing serial data's death Successfully !!!");
		}
		catch (InterruptedException e)
		{
			MsgAnalyzerCmnDef.WriteDebugSyslog("The analyzing serial data worker thread throws a InterruptedException...");
		}
		catch(Exception e)
		{
			MsgAnalyzerCmnDef.WriteErrorSyslog("Error occur while waiting for death of analyzing serial data worker thread, due to: " + e.toString());
		}

		return ret;
	}

	public short add_serial_data(String new_serial_data, boolean new_highlight)
	{
		synchronized(this)
		{
			serial_data = new_serial_data;
			highlight = new_highlight;
			notify();
		}

		return MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
	}

	@Override
	public void run() 
	{
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		
		// TODO Auto-generated method stub
		while (!exit.get())
		{
			try
			{
				synchronized(this)
				{
					wait(); // Wait for the signal, the thread should be created first
					// Parse each data from the serial port
					ret = MsgAnalyzerCmnDef.parse_serial_parameter(serial_data, highlight, MsgAnalyzerCmnDef.SHOW_DEVICE_CONSOLE);
					if (MsgAnalyzerCmnDef.CheckFailure(ret))
						break;
				}
			}
			catch (InterruptedException e)
			{
				MsgAnalyzerCmnDef.WriteDebugSyslog("The analyzing serial data worker thread throws a InterruptedException...");
			}
			catch(Exception e)
			{
				MsgAnalyzerCmnDef.WriteErrorSyslog("Error occur while waiting for death of analyzing serial data worker thread, due to: " + e.toString());
				ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_UNKNOWN;
				break;
			}
		}
		MsgAnalyzerCmnDef.WriteDebugSyslog("The worker thread of analyzing message is dead !!!");
	}
}
