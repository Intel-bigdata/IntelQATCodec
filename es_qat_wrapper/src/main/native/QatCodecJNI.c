/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define _GNU_SOURCE
#include <jni.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <dlfcn.h>

#include <stdlib.h>
#include <string.h>

#include <stdint.h>

#include "qatzip.h"

#include <stdio.h>

/* A helper macro to 'throw' a java exception. */
#define THROW(env, exception_name, message) \
{ \
    jclass ecls = (*env)->FindClass(env, exception_name); \
    if (ecls) { \
        (*env)->ThrowNew(env, ecls, message); \
        (*env)->DeleteLocalRef(env, ecls); \
    } \
}

#define QAT_ZIP_LIBRARY_NAME "libqatzip.so"

typedef int (*dlsym_qzCompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* src_len, unsigned char* dest, unsigned int* dest_len,
    unsigned int last);
typedef int (*dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
typedef int (*dlsym_qzGetDefaults)(QzSessionParams_T *defaults);
typedef int (*dlsym_qzSetDefaults)(QzSessionParams_T *defaults);


typedef struct qat_wrapper_context {
     int magic;
    dlsym_qzCompress compress;
    dlsym_qzDecompress decompress;
    dlsym_qzGetDefaults getDefaults;
    dlsym_qzSetDefaults setDefaults;
} qat_wrapper_context_t;

qat_wrapper_context_t g_qat_wrapper_context;

__thread QzSession_T  g_qzSession = {
    .internal = NULL,
};

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_intel_qat_jni_QatCodecJNI_init(
        JNIEnv *env, jclass cls)
{
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
    void *lib = dlopen(QAT_ZIP_LIBRARY_NAME, RTLD_LAZY | RTLD_GLOBAL);
    if (!lib)
    {
        char msg[128];
        snprintf(msg, 128, "Can't load %s due to %s", QAT_ZIP_LIBRARY_NAME, dlerror());
        THROW(env, "java/lang/UnsatisfiedLinkError", msg);
    }

    dlerror(); // Clear any existing error

    qat_wrapper_context->compress = dlsym(lib, "qzCompress");
    if (qat_wrapper_context->compress == NULL)
    {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzCompress");
    }

    qat_wrapper_context->decompress = dlsym(lib, "qzDecompress");
    if (qat_wrapper_context->compress == NULL)
    {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzDecompress");
    }

    qat_wrapper_context->getDefaults = dlsym(lib, "qzGetDefaults");
    if (qat_wrapper_context->getDefaults == NULL)
    {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzGetDefaults");
    }

    qat_wrapper_context->setDefaults = dlsym(lib, "qzSetDefaults");
    if (qat_wrapper_context->setDefaults == NULL)
    {
        THROW(env, "java/lang/UnsatisfiedLinkError", "Failed to load qzSetDefaults");
    }
}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    allocNativeBuffer
 * Signature: (II)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_com_intel_qat_jni_QatCodecJNI_allocNativeBuffer(
        JNIEnv *env, jclass cls, jint capacity, jint align)
{
    void *buffer = NULL;
    posix_memalign (&buffer, align, capacity);
    if (buffer != NULL)
    {
        return (*env)->NewDirectByteBuffer(env, buffer, capacity);
    }
    else
    {
        THROW(env, "java/lang/OutOfMemoryError", "Error alloc the native buffer");
        return NULL;
    }
}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    createCompressContext
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_com_intel_qat_jni_QatCodecJNI_createCompressContext(
        JNIEnv *env, jclass cls, jint level)
{
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;
    QzSessionParams_T params;
    qat_wrapper_context->getDefaults(&params);
    params.comp_lvl = level;
    //fprintf(stderr, "compression level is %d, tid is %ld\n", level, syscall(__NR_gettid));
    qat_wrapper_context->setDefaults(&params);
    return (jlong)1;
}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    createDecompressContext
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_intel_qat_jni_QatCodecJNI_createDecompressContext(
        JNIEnv *env, jclass cls)
{
    return (jlong)1;
}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    destroyContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_intel_qat_jni_QatCodecJNI_destroyContext(
        JNIEnv *env, jclass cls, jlong contextFromJava)
{

}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    compress
 * Signature: (JLjava/nio/ByteBuffer;IILjava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL
Java_com_intel_qat_jni_QatCodecJNI_compress(
        JNIEnv *env, jclass cls, jlong contextFromJava,
        jobject srcBuffer, jint srcOff, jint srcLen,
        jobject destBuffer, jint destOff, jint destLen)
{

    uint8_t* in;
    uint8_t* out;
    uint32_t uncompressed_size = 0;
    uint32_t compressed_size = 0;
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;

    in = (uint8_t*)(*env)->GetDirectBufferAddress(env, srcBuffer);
    if (in == NULL)
    {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get compressor input buffer");
    }

    out = (uint8_t*)(*env)->GetDirectBufferAddress(env, destBuffer);

    if (out == NULL)
    {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get compressor output buffer");
    }

    in += srcOff;
    out += destOff;

    uncompressed_size = srcLen;
    compressed_size = destLen;
    int ret = qat_wrapper_context->compress(&g_qzSession, in, &uncompressed_size, out, &compressed_size, 1);
    if (ret == QZ_OK)
    {
    }
    else if (ret == QZ_PARAMS)
    {
        THROW(env, "java/lang/InternalError", "Could not compress data. *sess is NULL or member of params is invalid.");
    }
    else if (ret == QZ_FAIL)
    {
        THROW(env, "java/lang/InternalError", "Could not compress data. Function did not succeed.");
    }
    else
    {
        char temp[256];
        sprintf(temp, "Could not compress data. Return error code %d", ret);
        THROW(env, "java/lang/InternalError", temp);
    }

    return compressed_size;
}

/*
 * Class:     com_intel_qat_jni_QatCodecJNI
 * Method:    decompress
 * Signature: (JLjava/nio/ByteBuffer;IILjava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL
Java_com_intel_qat_jni_QatCodecJNI_decompress(
        JNIEnv *env, jclass cls, jlong contextFromJava,
        jobject srcBuffer, jint srcOff, jint srcLen,
        jobject destBuffer, jint destOff, jint destLen)
{

    uint8_t* in;
    uint8_t* out;
    uint32_t uncompressed_size = 0;
    uint32_t compressed_size = 0;
    qat_wrapper_context_t *qat_wrapper_context = &g_qat_wrapper_context;

    in = (uint8_t*)(*env)->GetDirectBufferAddress(env, srcBuffer);
    if (in == NULL)
    {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get decompressor input buffer");
    }

    out = (uint8_t*)(*env)->GetDirectBufferAddress(env, destBuffer);
    if (out == NULL)
    {
        THROW(env, "java/lang/OutOfMemoryError", "Can't get decompressor output buffer");
    }

    in += srcOff;
    out += destOff;

    compressed_size = srcLen;
    uncompressed_size = destLen;
    int ret = qat_wrapper_context->decompress(&g_qzSession, in, &compressed_size, out, &uncompressed_size);
    if (ret == QZ_OK)
    {
    }
    else if (ret == QZ_PARAMS)
    {
        THROW(env, "java/lang/InternalError", "Could not decompress data. *sess is NULL or member of params is invalid");
    }
    else if (ret == QZ_FAIL)
    {
        THROW(env, "java/lang/InternalError", "Could not decompress data. Function did not succeed.");
    }
    else
    {
        char temp[256];
        sprintf(temp, "Could not decompress data. Return error code %d", ret);
        THROW(env, "java/lang/InternalError", temp);
    }

    return uncompressed_size;
}
