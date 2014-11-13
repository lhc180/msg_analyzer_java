package com.price.msg_analyzer;

import gnu.io.*;
import java.io.*;
import java.util.*;

/*
 *  When I load Arduino from the terminal it outputs various things to the prompt. Normally it's just the print stuff but now it opens with "RXTX Warning: 
 *  Removing stale lock file. /var/lock/LCK..ttyS0". I can upload to the Arduino after doing the simlink ACM0 -> S0 but when I try to use the serial monitor to 
 *  listen to what it sends back the IDE crashes and I get the following error :

    RXTX Warning: Removing stale lock file. /var/lock/LCK..ttyS0
    #
    # A fatal error has been detected by the Java Runtime Environment:
    #
    # SIGSEGV (0xb) at pc=0x00007f7dd5209d9d, pid=8206, tid=140178415417088
    #
    # JRE version: 6.0_24-b24
    # Java VM: OpenJDK 64-Bit Server VM (20.0-b12 mixed mode linux-amd64 compressed oops)
    # Derivative: IcedTea6 1.11.4
    # Distribution: Ubuntu 12.04 LTS, package 6b24-1.11.4-1ubuntu0.12.04.1
    # Problematic frame:
    # C [librxtxSerial.so+0x6d9d] read_byte_array+0x3d
    #
    # An error report file with more information is saved as:
    # /tmp/hs_err_pid8206.log
    #
    # If you would like to submit a bug report, please include
    # instructions how to reproduce the bug and visit:
    # https://bugs.launchpad.net/ubuntu/ source/openjdk-6/
    # The crash happened outside the Java Virtual Machine in native code.
    # See problematic frame for where to report the bug.
    #
    /usr/bin/arduino: line 33: 8206 Aborted (core dumped) java -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel processing.app.Base
    Clearly there's some issue between the RXTX library and the ttyACM0 and ttyS0 connections :( The simlink fixes one problem and makes another. If I use sudo to open arduino I don't get the lock file problem but it still crashes when I listen to the serial monitor.
     
##############################################################################################################################################################

    The problem with file lock is likely due to the lock-file being created with root permissions (sudo), however the IDE is being ran under user mode. 
    Either you will have to make the lock file create in user-mode, change the ownership of the lock, or run the IDE in root. 
    Running it in root shouldn't be a big issue. [edit: ahh, see you did this]

    The second problem looks like an ABI issue with the shared objects you used, they were compiled in 2008! That's an eternity in ABI compatibility years. 
    I would suggest just building the shared objects. Additionally these binaries were built for Centos 5.2 [RHEL] rather than any Ubuntu version [Debian]. 
    I'd suggest building them from scratch and then check if you get the same issue. (build instructions were listed above) 
   
   For more detailed: http://www.sciforums.com/threads/java-ubuntu-and-reading-serial-ports.118936/
 */


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
			synchronized(this)
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
		synchronized(this)
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
				MsgAnalyzerCmnDef.WriteDebugFormatSyslog("An InterruptedException is thrown when reading the serial data...");
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
