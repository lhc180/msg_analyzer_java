#include "serial_port_jni_mgr.h"
#include "serial_port_cmn_def.h"
#include "syslog_debug.h"


SerialPortJniMgr::SerialPortJniMgr() :
	fd(0)
{
	memset(device_file, 0x0, sizeof(char) * 256);
}

unsigned short SerialPortJniMgr::get_supported_baudrate(unsigned int serial_baudrate, tcflag_t& baudrate)
{
	switch (serial_baudrate)
	{
	case 9600:
		baudrate = B9600;
		break;
	case 115200:
		baudrate = B115200;
		break;
	default:
		return SERIAL_PORT_FAILURE_INVALID_ARGUMENT;
	}
	return SERIAL_PORT_SUCCESS;
}

unsigned short SerialPortJniMgr::open_serial(const char* serial_device_file, int serial_baudrate)
{
	if (serial_device_file == NULL)
	{
		WRITE_ERR_SYSLOG("Invalid argument: serial_device_file\n");
		return SERIAL_PORT_FAILURE_INVALID_ARGUMENT;
	}
	unsigned short ret = get_supported_baudrate(serial_baudrate, baudrate);
	if (CHECK_SERIAL_PORT_FAILURE(ret))
	{
		WRITE_ERR_FORMAT_SYSLOG("Unsupported baud rate: %d\n", serial_baudrate);
		return ret;
	}

	WRITE_DEBUG_FORMAT_SYSLOG("Try to open a serial port[%s] with baud rate: %d\n", serial_device_file, serial_baudrate);
	memcpy(device_file, serial_device_file, sizeof(char) * strlen(serial_device_file));
// Open modem device for reading and writing and not as controlling tty because we don't want to get killed if line noise sends CTRL-C.
	fd = open(device_file, O_RDWR | O_NOCTTY);
	if (fd <0)
	{
		WRITE_ERR_FORMAT_SYSLOG("open() failed, due to %s\n", strerror(errno));
		return SERIAL_PORT_FAILURE_OPEN_FILE;
	}

	tcgetattr(fd, &oldtio); /* save current serial port settings */
	bzero(&newtio, sizeof(newtio)); /* clear struct for new port settings */

//	BAUDRATE: Set bps rate. You could also use cfsetispeed and cfsetospeed.
//	CRTSCTS : output hardware flow control (only used if the cable has all necessary lines. See sect. 7 of Serial-HOWTO)
//	CS8     : 8n1 (8bit,no parity,1 stopbit)
//	CLOCAL  : local connection, no modem control
//	CREAD   : enable receiving characters

	newtio.c_cflag = baudrate | CRTSCTS | CS8 | CLOCAL | CREAD;

//	IGNPAR  : ignore bytes with parity errors
//	ICRNL   : map CR to NL (otherwise a CR input on the other computer will not terminate input), otherwise make device raw (no other input processing)
	newtio.c_iflag = IGNPAR | ICRNL;

// Raw output.
	newtio.c_oflag = 0;

// ICANON  : enable canonical input, disable all echo functionality, and don't send signals to calling program
	newtio.c_lflag = ICANON;

//	initialize all control characters default values can be found in /usr/include/termios.h, and are given in the comments, but we don't need them here
	newtio.c_cc[VINTR]    = 0;     /* Ctrl-c */
	newtio.c_cc[VQUIT]    = 0;     /* Ctrl-\ */
	newtio.c_cc[VERASE]   = 0;     /* del */
	newtio.c_cc[VKILL]    = 0;     /* @ */
	newtio.c_cc[VEOF]     = 4;     /* Ctrl-d */
	newtio.c_cc[VTIME]    = 0;     /* inter-character timer unused */
	newtio.c_cc[VMIN]     = 1;     /* blocking read until 1 character arrives */
	newtio.c_cc[VSWTC]    = 0;     /* '\0' */
	newtio.c_cc[VSTART]   = 0;     /* Ctrl-q */
	newtio.c_cc[VSTOP]    = 0;     /* Ctrl-s */
	newtio.c_cc[VSUSP]    = 0;     /* Ctrl-z */
	newtio.c_cc[VEOL]     = 0;     /* '\0' */
	newtio.c_cc[VREPRINT] = 0;     /* Ctrl-r */
	newtio.c_cc[VDISCARD] = 0;     /* Ctrl-u */
	newtio.c_cc[VWERASE]  = 0;     /* Ctrl-w */
	newtio.c_cc[VLNEXT]   = 0;     /* Ctrl-v */
	newtio.c_cc[VEOL2]    = 0;     /* '\0' */

// now clean the modem line and activate the settings for the port
	tcflush(fd, TCIFLUSH);
	tcsetattr(fd, TCSANOW, &newtio);

	return SERIAL_PORT_SUCCESS;
}

unsigned short SerialPortJniMgr::close_serial()
{
	if (fd != 0)
	{
		tcsetattr(fd, TCSANOW, &oldtio);

		close(fd);
		fd = 0;
	}

	return SERIAL_PORT_SUCCESS;
}

unsigned short SerialPortJniMgr::read_serial(char* buf, int buf_size, int& actual_datalen)
{
	if (buf == NULL)
	{
		WRITE_ERR_SYSLOG("Invalid argument: buf");
		return SERIAL_PORT_FAILURE_INVALID_ARGUMENT;
	}

	WRITE_DEBUG_SYSLOG("Check1");
	actual_datalen = read(fd, buf, buf_size);
	WRITE_DEBUG_SYSLOG("Check2");
	WRITE_DEBUG_FORMAT_SYSLOG("read data: %s, len: %d\n", buf, buf_size);
	if (actual_datalen == -1)
	{
		WRITE_ERR_FORMAT_SYSLOG("read() failed, due to %s\n", strerror(errno));
		return SERIAL_PORT_FAILURE_UNKNOWN;
	}
	WRITE_DEBUG_SYSLOG("Check3");

	return SERIAL_PORT_SUCCESS;
}

