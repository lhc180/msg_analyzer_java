package com.price.msg_analyzer;

import gnu.io.*;
import java.io.*;
import java.util.*;


public class SerialPortWrapper implements SerialPortEventListener
{
	private static final String COM_PORT_NAME = "/dev/ttyUSB0";
	private static final int COM_PORT_SPEED = 115200;
	private static final int TIME_OUT = 2000;

	private SerialReceiver serial_receiver = null;
	// The opened port
    private SerialPort serialPort = null;
    private CommPortIdentifier portId = null;

	// A BufferedReader which will be fed by a InputStreamReader converting the bytes into characters making 
    // the displayed results code page independent
	private BufferedReader input;

	public SerialPortWrapper(SerialReceiver srl_receiver)
	{
		serial_receiver = srl_receiver;
	}
	
	@Override
	public void serialEvent(SerialPortEvent oEvent)
	{
// Handle an event on the serial port. Read the data and print it.
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) 
		{
			synchronized(serial_receiver)
			{
// Wake up waiting thread with condition variable, if it is called before this function
				notify();
			}
		}
	}

	public short open_serial()
	{
		 // the next line is for Raspberry Pi and gets us into the while loop and was suggested here was suggested http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
        System.setProperty("gnu.io.rxtx.SerialPorts", COM_PORT_NAME);

		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) 
		{
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			if (currPortId.getName().equals(COM_PORT_NAME))
			{
				portId = currPortId;
				break;
			}
		}
		if (portId == null) 
		{
			MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Fail to find the COM port: %s", COM_PORT_NAME);
			return MsgAnalyzerCmnDef.ANALYZER_FAILURE_HANDLE_SERIAL;
		}

		try 
		{
// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
// set port parameters
			serialPort.setSerialPortParams(COM_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

// open the output streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));

// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} 
		catch (Exception e) 
		{
			MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Fail to open the COM port[%s] , due to: %s", COM_PORT_NAME, e.toString());
		}

		return MsgAnalyzerCmnDef.ANALYZER_SUCCESS;	
	}

	public short close_serial()
	{
		if (serialPort != null) 
		{
			serialPort.removeEventListener();
			serialPort.close();
			serialPort = null;
		}
		portId = null;


		return MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
	}

	public short read_serial(StringBuilder buf)
	{
		synchronized(serial_receiver)
		{
			try 
			{
// wait for input data
				wait();

				String inputLine = input.readLine();
				buf.append(inputLine);
//				System.out.println(inputLine);
			} 
			catch (InterruptedException e) 
			{
//				System.err.println(e.toString());
				MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Exception occur while waiting for the serial data, due to: %s", e.toString());
			}
			catch (IOException e) 
			{
//				System.err.println(e.toString());
				MsgAnalyzerCmnDef.WriteErrorFormatSyslog("Exception occur while reading the serial data, due to: %s", e.toString());
			}
		}

		return MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
	}

	public short write_serial()
	{
		return MsgAnalyzerCmnDef.ANALYZER_SUCCESS;
	}

}
