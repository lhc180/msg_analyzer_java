#ifndef SERIAL_PORT_LIB_H
#define SERIAL_PORT_LIB_H

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// APIs

extern "C"
{

void serial_port_get_version(unsigned char& major_version, unsigned char& minor_version);
unsigned short serial_port_open_serial(const char* serial_device_file, int serial_baudrate);
unsigned short serial_port_close_serial();
unsigned short serial_port_read_serial(char* buf, int buf_size, int& actual_datalen);
unsigned short serial_port_write_serial();

}

typedef void (*FP_serial_port_get_version)(unsigned char& major_version, unsigned char& minor_version);
typedef unsigned short (*FP_serial_port_open_serial)(const char* serial_device_file, int serial_baudrate);
typedef unsigned short (*FP_serial_port_close_serial)();
typedef unsigned short (*FP_serial_port_read_serial)(char* buf, int buf_size, int& actual_datalen);
typedef unsigned short (*FP_serial_port_write_serial)();


#endif
