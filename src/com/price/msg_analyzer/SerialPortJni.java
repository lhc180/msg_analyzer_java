package com.price.msg_analyzer;

public class SerialPortJni 
{
	static
	{
		System.loadLibrary("msg_dumper_serial");
	}
	public native short initialize();
	public native short deinitialize();
	public native short open_serial(String serial_device_file, int serial_baudrate);
	public native short close_serial();
	public native short read_serial(StringBuilder buf, int expected_len, int[] actual_len);
}
