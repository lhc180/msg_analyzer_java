/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_price_msg_analyzer_SerialPortJni */

#ifndef _Included_com_price_msg_analyzer_SerialPortJni
#define _Included_com_price_msg_analyzer_SerialPortJni
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    initialize
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_initialize
  (JNIEnv *, jobject);

/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    deinitialize
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_deinitialize
  (JNIEnv *, jobject);

/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    open_serial
 * Signature: (Ljava/lang/String;I)S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_open_1serial
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    close_serial
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_close_1serial
  (JNIEnv *, jobject);

/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    read_serial
 * Signature: (Ljava/lang/StringBuilder;)S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_read_1serial
  (JNIEnv *, jobject, jobject);

/*
 * Class:     com_price_msg_analyzer_SerialPortJni
 * Method:    write_serial
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_com_price_msg_1analyzer_SerialPortJni_write_1serial
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
