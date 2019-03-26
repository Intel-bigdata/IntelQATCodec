#include "com_intel_qat_jni_CarbondataQatJNI.h"
#include "qatzip.h"

#include <sys/syscall.h>
#include <unistd.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#define QAT_ZIP_LIBRARY_NAME "libqatzip.so"

/* A helper macro to 'throw' a java exception. */
#define THROW(env, exception_name, message) \
{ \
    jclass ecls = (*env)->FindClass(env, exception_name); \
    if (ecls) { \
        (*env)->ThrowNew(env, ecls, message); \
        (*env)->DeleteLocalRef(env, ecls); \
    } \
}

typedef int (*dlsym_qzCompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* src_len, unsigned char* dest, unsigned int* dest_len,
    unsigned int last);
typedef int (*dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
typedef int (*dlsym_qzGetDefaults)(QzSessionParams_T *defaults);
typedef unsigned int (*dlsym_qzMaxCompressedLength)(unsigned int src_sz);


typedef struct qat_wrapper_context {
    dlsym_qzCompress compress;
    dlsym_qzDecompress decompress;
    dlsym_qzGetDefaults getDefaults;
    dlsym_qzMaxCompressedLength maxCompressedLength;
} qat_wrapper_context_t;

qat_wrapper_context_t g_qat_wrapper_context;


__thread QzSession_T  g_qzSession = {
    .internal = NULL,
};

JNIEXPORT void JNICALL Java_com_intel_qat_jni_CarbondataQatJNI_init
  (JNIEnv *env, jclass cls) {

	qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
    void *lib = dlopen(QAT_ZIP_LIBRARY_NAME, RTLD_LAZY | RTLD_GLOBAL);
    if (!lib){
        char msg[128];
        snprintf(msg, 128, "Can't load %s due to %s", QAT_ZIP_LIBRARY_NAME, dlerror());
        THROW(env, "java/lang/UnsatisfiedLinkError", msg);
    }

    dlerror(); // Clear any existing error

    qat_wrapper_context->compress = dlsym(lib, "qzCompress");
    if (qat_wrapper_context->compress == NULL) {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzCompress");
    }

    qat_wrapper_context->decompress = dlsym(lib, "qzDecompress");
    if (qat_wrapper_context->compress == NULL) {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzDecompress");
    }

    qat_wrapper_context->getDefaults = dlsym(lib, "qzGetDefaults");
    if (qat_wrapper_context->getDefaults == NULL) {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzGetDefaults");
    }

    qat_wrapper_context->maxCompressedLength = dlsym(lib, "qzMaxCompressedLength");
    if (qat_wrapper_context->maxCompressedLength == NULL) {
        THROW(env, "java/lang/UnsatisfiedLinkError", "why Failed to load qzMaxCompressedLength");
    }
}


JNIEXPORT jint JNICALL Java_com_intel_qat_jni_CarbondataQatJNI_compress
  (JNIEnv *env, jclass cls, jbyteArray src, jint srcLen, jbyteArray des){

    jbyte *in = (*env)->GetByteArrayElements(env, src, 0);
    if (in == NULL) {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get compressor input buffer");
    }

    jbyte *out = (*env)->GetByteArrayElements(env, des, 0);
    if (out == NULL) {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get compressor output buffer");
    }

    uint32_t uncompressed_size = srcLen;
    uint32_t compressed_size = srcLen+1000;
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
	int ret = qat_wrapper_context->compress(&g_qzSession, in, &uncompressed_size, out, &compressed_size, 1);
	if (ret == QZ_OK) {
    }
    else if (ret == QZ_PARAMS) {
        THROW(env, "java/lang/InternalError", "Could not compress data. *sess is NULL or member of params is invalid.");
    }
    else if (ret == QZ_FAIL) {
        THROW(env, "java/lang/InternalError", "Could not compress data. Function did not succeed.");
    }
    else {
        char temp[256];
        sprintf(temp, "Could not compress data. Return error code %d", ret);
        THROW(env, "java/lang/InternalError", temp);
    }

    (*env)->ReleaseByteArrayElements(env, src, in, JNI_COMMIT);
    (*env)->ReleaseByteArrayElements(env, des, out, JNI_COMMIT);

    return compressed_size;
}


JNIEXPORT jint JNICALL Java_com_intel_qat_jni_CarbondataQatJNI_decompress
  (JNIEnv *env, jclass cls, jbyteArray src, jint srcOff, jint srcLen, jbyteArray des){

	jbyte *in = (*env)->GetByteArrayElements(env, src, 0);
    if (in == NULL) {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get decompressor input buffer");
    }
    in += srcOff;

    jbyte *out = (*env)->GetByteArrayElements(env, des, 0);
    if (out == NULL) {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get decompressor output buffer");
    }

    uint32_t compressed_size = srcLen;
    uint32_t uncompressed_size = srcLen*2;
    
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
    int ret = qat_wrapper_context->decompress(&g_qzSession, in, &compressed_size, out, &uncompressed_size);
    if (ret == QZ_OK) {
    }
    else if (ret == QZ_PARAMS) {
        THROW(env, "java/lang/InternalError", "Could not decompress data. *sess is NULL or member of params is invalid");
    }
    else if (ret == QZ_FAIL) {
        THROW(env, "java/lang/InternalError", "Could not decompress data. Function did not succeed.");
    }
    else {
        char temp[256];
        sprintf(temp, "Could not decompress data. Return error code %d", ret);
        THROW(env, "java/lang/InternalError", temp);
    }

    (*env)->ReleaseByteArrayElements(env, src, in, JNI_COMMIT);
    (*env)->ReleaseByteArrayElements(env, des, out, JNI_COMMIT);
    // if ((*env)->ExceptionCheck(env)){
    //     THROW(env, "java/lang/InternalError", "Could not release array. Function did not succeed.");
    // }
    return uncompressed_size;
}


JNIEXPORT jint JNICALL Java_com_intel_qat_jni_CarbondataQatJNI_maxCompressedLength
  (JNIEnv *env, jclass cls, jint length){

	qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
    uint32_t ret = qat_wrapper_context->maxCompressedLength((uint32_t)length);
    return ret;
}

