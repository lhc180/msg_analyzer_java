#ifndef SERIAL_PORT_JNI_MGR_H
#define SERIAL_PORT_JNI_MGR_H

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>
#include <unistd.h>
#include <termios.h>
#include <errno.h>
#include <fcntl.h>


class SerialPortJniMgr
{
private:
	static unsigned short get_supported_baudrate(unsigned int serial_baudrate, tcflag_t& baudrate);

	int fd;
	struct termios oldtio;
	struct termios newtio;
	char device_file[256];
	tcflag_t baudrate;

public:
	SerialPortJniMgr();

	unsigned short open_serial(const char* serial_device_file, int serial_baudrate);
	unsigned short close_serial();
	unsigned short read_serial(char* buf, int buf_size, int& actual_datalen);
};

#endif
