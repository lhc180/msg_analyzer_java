package com.price.msg_analyzer;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;


public class SerialReceiver extends MsgAnalyzerCmnBase implements Runnable
{
	static int BUF_SIZE = 256;
	static int WAIT_TIMEOUT = 10;
	static final String title[] = {"device_file", "baud_rate"};
	static int title_len = title.length;

	private SerialAnalyzer serial_analyzer = null;
	private Thread t = null;
	private SerialPortJni serial_port = null;
	private AtomicBoolean exit = new AtomicBoolean(false);
	private boolean device_handle_exist = false;

	private String device_file = MsgAnalyzerCmnDef.DEF_DEVICE_FILE;
	private int baud_rate = MsgAnalyzerCmnDef.DEF_BAUD_RATE;

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
					scg_command_buf.append(new_serial_data.replaceAll("\\s", ""));
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_WAIT_SCG_COMMAND;
				}
				return ret;
			};
		}
	};

	public SerialReceiver(SerialAnalyzer srl_analyzer)
	{
		serial_analyzer = srl_analyzer;
		serial_port = new SerialPortJni();
		t = new Thread(this);
	}

	short initialize()
	{
// Parse the config file
		short ret = parse_config();
		if (MsgAnalyzerCmnDef.CheckFailure(ret))
			return ret;

// Open the serial port
		MsgAnalyzerCmnDef.WriteDebugFormatSyslog("Open the serial port[%s, %d]", device_file, baud_rate);
		ret = serial_port.open_serial(device_file, baud_rate);
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
		if(!t.isAlive())
			MsgAnalyzerCmnDef.WriteDebugSyslog("The worker thread in the SerialReceiver object did NOT exist......");
		else
		{
			exit.set(true);
			MsgAnalyzerCmnDef.WriteDebugSyslog("The worker thread in the SerialReceiver object is STILL alive");
			t.interrupt();
			try 
			{
// wait till this thread dies
				t.join(2000);
				// System.out.println("The worker thread of receiving serial data is dead");
				MsgAnalyzerCmnDef.WriteDebugSyslog("The worker thread of receving serial data is dead");
			}
			catch (InterruptedException e)
			{
				MsgAnalyzerCmnDef.WriteDebugSyslog("The receiving serial data worker thread throws a InterruptedException...");
			}
			catch(Exception e)
			{
				MsgAnalyzerCmnDef.WriteErrorSyslog("Error occur while waiting for death of receving serial data worker thread, due to: " + e.toString());
			}
		}
		device_handle_exist = false;

		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
// Close the serial port
		MsgAnalyzerCmnDef.WriteDebugSyslog("De-initialize the SerialPortWrapper object");
		ret = serial_port.close_serial();
		if (MsgAnalyzerCmnDef.CheckFailure(ret))
			return ret;

		return ret;
	}

	short check_scg_command(String new_serial_data)
	{
		return parse_command_array[(int)scg_command_status.ordinal()].parse_command(new_serial_data);
	}

	short parse_config()
	{
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		BufferedReader reader = null;
		String conf_filepath = String.format("%s/%s/%s", MsgAnalyzerCmnDef.get_cur_path(), MsgAnalyzerCmnDef.CONF_FOLDER_NAME, MsgAnalyzerCmnDef.CONF_FILE_NAME);
		MsgAnalyzerCmnDef.WriteDebugFormatSyslog("Parse the config file: %s", conf_filepath);
		try
		{
			reader = new BufferedReader(new FileReader(conf_filepath));
			String line = null;

			while ((line = reader.readLine()) != null)
			{
				int split_pos = line.indexOf('=');

				if (line.isEmpty())
					break;
				else if (split_pos < 0)
				{
					MsgAnalyzerCmnDef.WriteErrorFormatSyslog(__FILE__(), __LINE__(), "Incorrect config: %s", line);
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_UNKNOWN;
					break;
				}

				boolean found = true;
				String param_title = line.substring(0, split_pos);
				String param_content = line.substring(split_pos + 1);
				for (int index = 0 ; index < title_len ; index++)
				{
					if (param_title.equals(title[index]))
					{
						switch (index)
						{
						case 0:
							device_file = param_content;
							break;
						case 1:
							baud_rate = Integer.parseInt(param_content);
							break;
						default:
							MsgAnalyzerCmnDef.WriteErrorFormatSyslog(__FILE__(), __LINE__(), "Incorrect parameter: %s=%s", param_title, param_content);
							ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_INVALID_ARGUMENT;
							found = false;
							break;
						}
						break;
					}
				}

// If the title is NOT found...
				if (!found)
				{
					MsgAnalyzerCmnDef.WriteErrorFormatSyslog(__FILE__(), __LINE__(), "Incorrect parameter, fail to find the title: %s", param_title);
					ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_INVALID_ARGUMENT;
					break;
				}
			}
		}
		catch(FileNotFoundException ex)
		{
			MsgAnalyzerCmnDef.WriteErrorFormatSyslog(__FILE__(), __LINE__(), "Fail to find the config file: %s", conf_filepath);
			ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_INVALID_ARGUMENT;
		}
		catch(IOException ex)
		{
			MsgAnalyzerCmnDef.WriteErrorFormatSyslog(__FILE__(), __LINE__(), "Fail to access config file, due to: %s", ex.toString());
			ret = MsgAnalyzerCmnDef.ANALYZER_FAILURE_INVALID_ARGUMENT;
		}
		return ret;
	}

	@Override
	public void run() 
	{	
		int count = 0;
		short ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		int buf_size = BUF_SIZE;
		int[] actual_datalen = new int[1];
		int new_serial_data_len;

		LABEL:
		while (!exit.get())
		{
			StringBuilder buf = new StringBuilder();
			ret = serial_port.read_serial(buf, buf_size, actual_datalen);
			if (MsgAnalyzerCmnDef.CheckFailure(ret))
				break;

			String new_serial_data = buf.toString();
// Ignore the data which are NOT interested in
			if (new_serial_data.length() == 1 && new_serial_data.charAt(0) == '\n')
				continue;

			// Check if the data is finished !!!
			if (actual_datalen[0] == buf_size && new_serial_data.charAt(buf_size - 1) != '\n')
			{
				MsgAnalyzerCmnDef.WriteDebugFormatSyslog("The buffer size[%d] is NOT enough", buf_size);
				int total_actual_datalen = buf_size;
				int total_buf_size = buf_size;
				while (true)
				{
					int old_total_buf_size = total_buf_size;
					total_buf_size <<= 1;

// Read the following data
					buf = new StringBuilder();
					ret = serial_port.read_serial(buf, old_total_buf_size, actual_datalen);
					if (MsgAnalyzerCmnDef.CheckFailure(ret))
						 break LABEL;
					MsgAnalyzerCmnDef.WriteDebugFormatSyslog("Read the data size: %d, actual length: %d", old_total_buf_size, actual_datalen);
					new_serial_data += buf.toString();

					if (!(actual_datalen[0] == old_total_buf_size && new_serial_data.charAt(total_buf_size - 1) != '\n'))
					{
						actual_datalen[0] += old_total_buf_size;
						MsgAnalyzerCmnDef.WriteDebugFormatSyslog("Get the full data, size: %d", actual_datalen[0]);
						break;
					}
				}
			}
			new_serial_data_len = new_serial_data.length() - 1; // Ignore the '\n' character in the last element of the array

			MsgAnalyzerCmnDef.WriteDebugFormatSyslog("New Data[%d], data length: %d, data: %s", ++count, new_serial_data_len, new_serial_data.substring(0, new_serial_data_len));
// Check the data content is correct
			ret  = check_data_content(new_serial_data.substring(0, new_serial_data_len));
			if (MsgAnalyzerCmnDef.CheckFailure(ret))
				break;
		}
		MsgAnalyzerCmnDef.WriteDebugSyslog("The worker thread of receiving message is dead !!!");
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
				ret = serial_analyzer.add_serial_data(new_serial_data, highlight); // Update the data
		}
		else
		{
			if (MsgAnalyzerCmnDef.CheckIgnoreData(ret) || MsgAnalyzerCmnDef.CheckWaitSCGCommand(ret))
				ret = MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
		}

		return ret;
	}
}
