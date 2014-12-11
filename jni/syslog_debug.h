#ifndef SYSLOG_DEBUG_H
#define SYSLOG_DEBUG_H

#include <syslog.h>


static const int BUF_SIZE = 256;

#define WRITE_SYSLOG_BEGIN()\
do{\
char title[64];\
snprintf(title, 64, "%s:%d", __FILE__, __LINE__);\
openlog(title, LOG_PID | LOG_CONS, LOG_USER);

#define WRITE_SYSLOG_END()\
closelog();\
}while(0)

#define WRITE_FORMAT_SYSLOG(priority, message_format, ...)\
WRITE_SYSLOG_BEGIN()\
char syslog_buf[BUF_SIZE];\
snprintf(syslog_buf, BUF_SIZE, message_format, __VA_ARGS__);\
syslog(priority, syslog_buf);\
WRITE_SYSLOG_END()

#define WRITE_DEBUG_FORMAT_SYSLOG(message_format, ...) WRITE_FORMAT_SYSLOG(LOG_DEBUG, message_format, __VA_ARGS__)
#define WRITE_INFO_FORMAT_SYSLOG(message_format, ...) WRITE_FORMAT_SYSLOG(LOG_INFO, message_format, __VA_ARGS__)
#define WRITE_ERR_FORMAT_SYSLOG(message_format, ...) WRITE_FORMAT_SYSLOG(LOG_ERR, message_format, __VA_ARGS__)


#endif
