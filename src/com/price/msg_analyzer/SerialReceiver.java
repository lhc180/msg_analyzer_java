package com.price.msg_analyzer;

import java.util.*;


public class SerialReceiver implements Runnable
{
	private SerialAnalyzer serial_analyzer = null;
	private Thread t = null;
	private SerialPortWrapper serial_port = null;
	private boolean device_handle_exist = false;
	private boolean exit = false;

	enum SCG_COMMAND_STATUS{SCG_COMMAND_NONE, SCG_COMMAND_DETECTED, SCG_COMMAND_PROCESS, SCG_COMMAND_COMPLETE};
	SCG_COMMAND_STATUS scg_command_status = SCG_COMMAND_STATUS.SCG_COMMAND_NONE;
	StringBuilder scg_command_buf;

	private interface ParseCommandInf {short parse_command(String new_serial_data);};
	ParseCommandInf[] parse_command_array = 
	{
		new ParseCommandInf()
		{
			String scg_command_data_check = "checkNewConfig 309 CONFIGURATIONS =>";
			@Override
			public short parse_command(String new_serial_data) 
			{
				short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
				int pos = new_serial_data.indexOf(scg_command_data_check);
				if (pos >= 0)
				{
					scg_command_buf = new StringBuilder(new_serial_data);
					scg_command_status = SCG_COMMAND_STATUS.SCG_COMMAND_DETECTED;
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_WAIT_SCG_COMMAND;
					MsgAnalyzerCmnDef.WriteDebugSyslog("A new SCG command is detected");
				}
				else
				{
// If the data is NOT SCG command, check if the serial data can be ignored
					int pos_colon = new_serial_data.indexOf(": ");
					if (pos_colon == -1)
					{
						MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Ignore incorrect data format(1): %s", new_serial_data);
						ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_IGNORE_DATA ;
					}
					else
					{
						if (pos_colon < MsgAnalyzerCmnDef.MIN_SERAIL_DATA_TITLE_LENGTH)
						{
							MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Ignore incorrect data format(2): %s", new_serial_data);
							ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_IGNORE_DATA;
						}
					}
				}
				return ret;
			};
		},
		new ParseCommandInf()
		{
			String scg_command_data_check = "{";
			@Override
			public short parse_command(String new_serial_data) 
			{
				short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
				int pos = new_serial_data.indexOf(scg_command_data_check);
				if (pos >= 0)
				{
					scg_command_buf.append(" {");
					scg_command_status = SCG_COMMAND_STATUS.SCG_COMMAND_PROCESS;
					MsgAnalyzerCmnDef.WriteDebugSyslog("The SCG command is started......");
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_WAIT_SCG_COMMAND;
				}
				else
				{
					MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Incorrect SCG command[%s], should start with \'{\'", new_serial_data);
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_INCORRECT_SERIAL_DATA;
				}
				
				return ret;
			};
		},
		new ParseCommandInf()
		{
			String scg_command_data_check = "}";
			@Override
			public short parse_command(String new_serial_data) 
			{
				short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
				int pos = new_serial_data.indexOf(scg_command_data_check);
				if (pos >= 0)
				{
					scg_command_status = SCG_COMMAND_STATUS.SCG_COMMAND_COMPLETE;
					MsgAnalyzerCmnDef.WriteDebugSyslog("The SCG command is completed......");
					scg_command_buf.append("}");
				}
				else
				{
			// Remove the space character...
//					new_serial_data.erase(remove(new_serial_data.begin(), new_serial_data.end(), ' '), new_serial_data.end());
					scg_command_buf.append(new_serial_data);
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_WAIT_SCG_COMMAND;
				}
				return ret;
			};
		}
	};
	
	public SerialReceiver(SerialAnalyzer srl_analyzer)
	{
		serial_analyzer = srl_analyzer;
		serial_port = new SerialPortWrapper(this);
		t = new Thread(this);
	}

	short initialize()
	{
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
// Open the serial port
		MsgAnalyzerCmnDef.WriteDebugFormatSyslog("Initialize the SerialPortWrapper object");
		ret = serial_port.open_serial();
		if (MsgAnalyzerCmnDef.CheckFailure(ret))
			return ret;
		device_handle_exist = true;

// Initialize the thread
		t.start();

		return ret;
	}

	short deinitialize()
	{
	// Before killing this thread, check if the thread is STILL alive
		if (!exit)
		{
			try
			{
				exit = true;
// Sleep so that the worker thread can write the the rest messages to the device
				Thread.sleep(2000);

				synchronized(this)
				{
					System.out.println("Notify the worker thread it's going to die...");
					notify();
				}
				t.join(3000);
				System.out.println("The thread is dead");
			}
			catch (InterruptedException e)
			{
				System.out.println("Error occur while waiting for thread's death: " + e.toString());
			}
			catch(Exception e)
			{
				System.out.println("Error occur: " + e.toString());
			}
		}
		device_handle_exist = false;

// Close the serial port
		MsgAnalyzerCmnDef.WriteDebugSyslog("De-initialize the SerialPortWrapper object");
		return serial_port.close_serial();
	}

	short check_scg_command(String new_serial_data)
	{
		return parse_command_array[scg_command_status.ordinal()].parse_command(new_serial_data);
	}

	@Override
	public void run() 
	{	
		int count = 0;
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		
		while (!exit)
		{
			StringBuilder buf = new StringBuilder();
			ret = serial_port.read_serial(buf);
			if (MsgAnalyzerCmnDef.CheckFailure(ret))
				break;

			String new_serial_data = buf.toString();
// Ignore the data which are NOT interested in
			if (new_serial_data.length() == 1 && new_serial_data.charAt(0) == '\n')
				continue;

			MsgAnalyzerCmnDef.WriteDebugFormatSyslog("New Data[%d], data length: %d, data: %s", ++count, new_serial_data.length(), new_serial_data);
// Check the data content is correct
			ret  = check_data_content(new_serial_data);
			if (MsgAnalyzerCmnDef.CheckFailure(ret))
				break;
		}	
	}

	short check_data_content(String new_serial_data)
	{
		short ret = check_scg_command(new_serial_data);
		if (MsgAnalyzerCmnDef.CheckSuccess(ret))
		{
			boolean highlight = false;
			if (scg_command_status == SCG_COMMAND_STATUS.SCG_COMMAND_COMPLETE)
			{
				new_serial_data = scg_command_buf.toString();
				highlight = true;
				scg_command_status = SCG_COMMAND_STATUS.SCG_COMMAND_NONE;
			}

			ret = MsgAnalyzerCmnDef.parse_serial_parameter(new_serial_data, highlight, MsgAnalyzerCmnDef.SHOW_DEVICE_SYSLOG);
			if (MsgAnalyzerCmnDef.CheckSuccess(ret))
			{
				// Update the data
				ret = serial_analyzer.add_serial_data(new_serial_data, highlight);
			}
		}
		else
		{
			if (MsgAnalyzerCmnDef.CheckIgnoreData(ret) || MsgAnalyzerCmnDef.CheckWaitSCGCommand(ret))
				ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		}

		return ret;
	}
}