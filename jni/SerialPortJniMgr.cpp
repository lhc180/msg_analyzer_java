#include "SerialPortJniMgr.h"
#include "msg_dumper.h"


char* SerialPortJniMgr::so_filename = "./libmsg_dumper.so";

SerialPortJniMgr::SerialPortJniMgr() :
	handle(NULL),
	is_init(false),
	fp_serial_port_get_version(NULL),
	fp_serial_port_open_serial(NULL),
	fp_serial_port_close_serial(NULL),
	fp_serial_port_read_serial(NULL)
{
}

unsigned short SerialPortJniMgr::initialize()
{
// Get the handle of the library
	handle = dlopen(so_filename, RTLD_NOW);
	if (handle == NULL)
	{
		fprintf(stderr, "dlopen() fails, due to %s\n", dlerror());
		return MSG_DUMPER_FAILURE_NOT_FOUND;
	}

// Export the APIs
	fp_serial_port_get_version = (FP_serial_port_get_version)dlsym(handle, "serial_port_get_version");
	if (fp_serial_port_get_version == NULL)
	{
//		fprintf(stderr, "dlsym() fails when exporting serial_port_get_version() due to %s\n", dlerror());
		return MSG_DUMPER_FAILURE_NOT_FOUND;
	}
	fp_serial_port_open_serial = (FP_serial_port_open_serial)dlsym(handle, "serial_port_open_serial");
	if (fp_serial_port_get_version == NULL)
	{
//		fprintf(stderr, "dlsym() fails when exporting serial_port_open_serial() due to %s\n", dlerror());
		return MSG_DUMPER_FAILURE_NOT_FOUND;
	}
	fp_serial_port_close_serial = (FP_serial_port_close_serial)dlsym(handle, "serial_port_close_serial");
	if (fp_serial_port_close_serial == NULL)
	{
//		fprintf(stderr, "dlsym() fails when exporting serial_port_close_serial() due to %s\n", dlerror());
		return MSG_DUMPER_FAILURE_NOT_FOUND;
	}
	fp_serial_port_read_serial = (FP_serial_port_read_serial)dlsym(handle, "serial_port_read_serial");
	if (fp_serial_port_read_serial == NULL)
	{
//		fprintf(stderr, "dlsym() fails when exporting serial_port_read_serial() due to %s\n", dlerror());
		return MSG_DUMPER_FAILURE_NOT_FOUND;
	}

	is_init = true;
	return MSG_DUMPER_SUCCESS;
}

unsigned short SerialPortJniMgr::deinitialize()
{
// Close the handle
	if (handle != NULL)
	{
		dlclose(handle);
		handle = NULL;
	}
	is_init = false;

	return MSG_DUMPER_SUCCESS;
}

unsigned short SerialPortJniMgr::get_version(unsigned char& major_version, unsigned char& minor_version)
{
	if (!is_init)
		return MSG_DUMPER_FAILURE_INCORRECT_OPERATION;

	serial_port_get_version(major_version, minor_version);
	return MSG_DUMPER_SUCCESS;
}

unsigned short SerialPortJniMgr::open_serial(const char* serial_device_file, int serial_baudrate)
{
	if (!is_init)
		return MSG_DUMPER_FAILURE_INCORRECT_OPERATION;

	return serial_port_open_serial(serial_device_file, serial_baudrate);
}

unsigned short SerialPortJniMgr::close_serial()
{
	if (!is_init)
		return MSG_DUMPER_FAILURE_INCORRECT_OPERATION;

	return serial_port_close_serial();
}

unsigned short SerialPortJniMgr::read_serial(char* buf, int buf_size, int& actual_datalen)
{
	if (!is_init)
		return MSG_DUMPER_FAILURE_INCORRECT_OPERATION;

	return serial_port_read_serial(buf, buf_size, actual_datalen);
}

