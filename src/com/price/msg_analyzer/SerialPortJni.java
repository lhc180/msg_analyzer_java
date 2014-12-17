package com.price.msg_analyzer;

public class SerialPortJni 
{
	static
	{
		System.loadLibrary("serial_port");
	}

	public native short open_serial(String serial_device_file, int serial_baudrate);
	public native short close_serial();
	public native short read_serial(StringBuilder buf, int expected_len, int[] actual_len);

	static final short SERIAL_PORT_SUCCESS = 0;

	static final short SERIAL_PORT_FAILURE_UNKNOWN = 1;
	static final short SERIAL_PORT_FAILURE_INVALID_ARGUMENT = 2;
	static final short SERIAL_PORT_FAILURE_INVALID_POINTER = 3;
	static final short SERIAL_PORT_FAILURE_INSUFFICIENT_MEMORY = 4;
	static final short SERIAL_PORT_FAILURE_OPEN_FILE = 5;
	static final short SERIAL_PORT_FAILURE_NOT_FOUND = 6;
	static final short SERIAL_PORT_FAILURE_INCORRECT_CONFIG = 7;
	static final short SERIAL_PORT_FAILURE_INCORRECT_OPERATION = 8;

	static boolean CheckSerialPortFailure(short x) {return (x != SERIAL_PORT_SUCCESS ? true : false);}

}
