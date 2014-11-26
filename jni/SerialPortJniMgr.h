#ifndef SERIAL_PORT_JNI_MGR_H
#define SERIAL_PORT_JNI_MGR_H

#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>
#include "serial_port_lib.h"


class SerialPortJniMgr
{
private:
	static char* so_filename;
	void* handle;
	bool is_init;

	FP_serial_port_get_version fp_serial_port_get_version;
	FP_serial_port_open_serial fp_serial_port_open_serial;
	FP_serial_port_close_serial fp_serial_port_close_serial;
	FP_serial_port_read_serial fp_serial_port_read_serial;

public:
	SerialPortJniMgr();

	unsigned short initialize();
	unsigned short deinitialize();
	unsigned short get_version(unsigned char& major_version, unsigned char& minor_version);
	unsigned short open_serial(const char* serial_device_file, int serial_baudrate);
	unsigned short close_serial();
	unsigned short read_serial(char* buf, int buf_size, int& actual_datalen);
};

#endif
