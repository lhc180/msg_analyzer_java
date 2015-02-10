#include "com_price_msg_analyzer_SerialPortJni.h"
#include <assert.h>
#include <string.h>
#include <iostream>
#include <string>
#include "syslog_debug.h"
#include "serial_port_jni_mgr.h"
#include "serial_port_cmn_def.h"


using namespace std;

static const char* StringBuilder_ClassName = "java/lang/StringBuilder";
static const char* StringBuilder_append_MethodName = "append";
static const char* StringBuilder_append_MethodSignature = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";

static jstring str2jstring(JNIEnv* env,const char* pat);
static string jstring2str(JNIEnv* env, jstring jstr);
static void init_StringBuilder_Append_method(JNIEnv* env);

static jclass StringBuilder_Class = 0;
static jmethodID StringBuilder_append_Method = 0;

SerialPortJniMgr serial_port_jni_mgr;

JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_open_1serial(JNIEnv *env, jobject jobj, jstring jdevice_file, jint jbaud_rate)
{
	string device_file = jstring2str(env, jdevice_file);
	int baud_rate = (int)jbaud_rate;

	WRITE_DEBUG_FORMAT_SYSLOG("(JNI) Open serial, device file: %s, baud rate: %d", device_file.c_str(), baud_rate);
	return serial_port_jni_mgr.open_serial(device_file.c_str(), baud_rate);
}

JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_close_1serial(JNIEnv *env, jobject jobj)
{
	return serial_port_jni_mgr.close_serial();
}

JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_read_1serial(JNIEnv *env, jobject jobj, jobject jbuf, jint jexpected_len, jintArray jactual_len)
{
	static int buf_size = 256;
	static char* buf = new char[buf_size];

	int expected_len = (int)jexpected_len;
	int* actual_len = env->GetIntArrayElements(jactual_len, 0);
// Expand the buffer size if not enough
	int old_buf_size = buf_size;
	while(buf_size < expected_len)
		buf_size <<= 1;
	if (buf_size != old_buf_size)
		buf = (char*)realloc(buf, buf_size);

	if (buf == NULL)
	{
		WRITE_ERR_SYSLOG("Insufficient memory: buf");
		return SERIAL_PORT_FAILURE_INSUFFICIENT_MEMORY;
	}

// Read the data from serial port
	memset(buf, 0x0, sizeof(char) * buf_size);
	short ret = serial_port_jni_mgr.read_serial(buf, expected_len, actual_len[0]);
	if (CHECK_SERIAL_PORT_FAILURE(ret))
		return ret;

// Transform into JAVA object
	WRITE_DEBUG_FORMAT_SYSLOG("(JNI) buf: %s; acutal len: %d", buf, actual_len[0]);
	init_StringBuilder_Append_method(env);
	jstring jString = env->NewStringUTF(buf);
// Because StringBuild.append() returns object, you should call CallObjectMethod
	env->CallObjectMethod(jbuf, StringBuilder_append_Method, jString);
// Release the array in JNI. Caution: it's necessary
	env->ReleaseIntArrayElements(jactual_len, actual_len, 0);

	return ret;
}

jstring str2jstring(JNIEnv* env,const char* pat)
{
	//定义java String类 strClass
	jclass strClass = env->FindClass("Ljava/lang/String;");
	//获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
	jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
	//建立byte数组
	jbyteArray bytes = env->NewByteArray(strlen(pat));
	//将char* 转换为byte数组
	env->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*)pat);
	// 设置String, 保存语言类型,用于byte数组转换至String时的参数
	jstring encoding = env->NewStringUTF("GB2312");
	//将byte数组转换为java String,并输出
	return (jstring)env->NewObject(strClass, ctorID, bytes, encoding);
}

string jstring2str(JNIEnv* env, jstring jstr)
{
	char* rtn = NULL;
	jclass clsstring = env->FindClass("java/lang/String");
	jstring strencode = env->NewStringUTF("GB2312");
	jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
	jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);
	jsize alen = env->GetArrayLength(barr);
	jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);
	if(alen > 0)
	{
		rtn = (char*)malloc(alen + 1);
		memcpy(rtn, ba, alen);
		rtn[alen]=0;
	}
	env->ReleaseByteArrayElements(barr, ba, 0);
	std::string stemp(rtn);
	free(rtn);
	return stemp;
}

void init_StringBuilder_Append_method(JNIEnv* env)
{
// Find the StringBuilder class
	if(StringBuilder_Class == NULL)
	{
		StringBuilder_Class = env->FindClass(StringBuilder_ClassName);
		// TODO: Handle error if class not found
		if (StringBuilder_Class == NULL)
		{
			WRITE_ERR_SYSLOG("StringBuilder_Class == NULL");
			assert(0 && "StringBuilder_Class == NULL");
		}
	}

// Find the append member function in the StringBuilder class
	if(StringBuilder_append_Method == NULL)
	{
		StringBuilder_append_Method = env->GetMethodID(StringBuilder_Class, StringBuilder_append_MethodName, StringBuilder_append_MethodSignature);
		// TODO: Handle error if method not found
		if (StringBuilder_append_Method == NULL)
		{
			WRITE_ERR_SYSLOG("StringBuilder_append_Method == NULL");
			assert(0 && "StringBuilder_append_Method == NULL");
		}
	}
}
