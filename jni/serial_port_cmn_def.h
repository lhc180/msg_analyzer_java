#ifndef SERIAL_PORT_CMN_DEF_H
#define SERIAL_PORT_CMN_DEF_H

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Return values
static const unsigned short SERIAL_PORT_SUCCESS = 0;

static const unsigned short SERIAL_PORT_FAILURE_UNKNOWN = 1;
static const unsigned short SERIAL_PORT_FAILURE_INVALID_ARGUMENT = 2;
static const unsigned short SERIAL_PORT_FAILURE_INVALID_POINTER = 3;
static const unsigned short SERIAL_PORT_FAILURE_INSUFFICIENT_MEMORY = 4;
static const unsigned short SERIAL_PORT_FAILURE_OPEN_FILE = 5;
static const unsigned short SERIAL_PORT_FAILURE_NOT_FOUND = 6;
static const unsigned short SERIAL_PORT_FAILURE_INCORRECT_CONFIG = 7;
static const unsigned short SERIAL_PORT_FAILURE_INCORRECT_OPERATION = 8;

#define CHECK_SERIAL_PORT_FAILURE(x) (x != SERIAL_PORT_SUCCESS ? true : false)


#endif
