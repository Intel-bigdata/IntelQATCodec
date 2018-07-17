/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "qatcodec.h"

#if defined HADOOP_QAT_LIBRARY

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>

#include "org_apache_hadoop_io_compress_qat_QatDecompressor.h"


static jfieldID QatDecompressor_clazz;
static jfieldID QatDecompressor_compressedDirectBuf;
static jfieldID QatDecompressor_compressedDirectBufLen;
static jfieldID QatDecompressor_uncompressedDirectBuf;
static jfieldID QatDecompressor_directBufferSize;

#define qaePinnedMemAlloc(x, y)  qaeMemAllocNUMA((x), (y), 8)
#define qaePinnedMemFree(x)      qaeMemFreeNUMA((void **)&(x))

#ifdef UNIX
static int (*dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
unsigned char* (*dlsym_qzMalloc)(int sz, int numa, int force_pinned);
#endif

#ifdef WINDOWS
typedef int (__cdecl *__dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
static __dlsym_qzDecompress dlsym_qzDecompress;
#endif

JNIEXPORT void JNICALL Java_org_apache_hadoop_io_compress_qat_QatDecompressor_initIDs
(JNIEnv *env, jclass clazz){

  // Load libqatzip.so
#ifdef UNIX
  void *libqatzip = dlopen("libqatzip.so", RTLD_LAZY | RTLD_GLOBAL);
  if (!libqatzip) {
    char msg[128];
    snprintf(msg, sizeof(msg), "%s (%s)!", "Cannot load " HADOOP_QAT_LIBRARY, dlerror());
    THROW(env, "java/lang/UnsatisfiedLinkError", msg);
    return;
  }
#endif

#ifdef WINDOWS
  HMODULE libqatzip = LoadLibrary(HADOOP_QAT_LIBRARY);
  if (!libqatzip) {
    THROW(env, "java/lang/UnsatisfiedLinkError", "Cannot load qatzip.dll");
    return;
  }
#endif

  // Locate the requisite symbols from libqatzip.so
#ifdef UNIX
  dlerror();                                 // Clear any existing error
  LOAD_DYNAMIC_SYMBOL(dlsym_qzDecompress, env, libqatzip, "qzDecompress");
  LOAD_DYNAMIC_SYMBOL(dlsym_qzMalloc, env, libqatzip, "qzMalloc");

#endif

#ifdef WINDOWS
  LOAD_DYNAMIC_SYMBOL(__dlsym_qatzip_uncompress, dlsym_qzDecompress, env, libqatzip, "qzDecompress");
#endif

  QatDecompressor_clazz = (*env)->GetStaticFieldID(env, clazz, "clazz",
                                                   "Ljava/lang/Class;");
  QatDecompressor_compressedDirectBuf = (*env)->GetFieldID(env,clazz,
                                                           "compressedDirectBuf",
                                                           "Ljava/nio/Buffer;");
  QatDecompressor_compressedDirectBufLen = (*env)->GetFieldID(env,clazz,
                                                              "compressedDirectBufLen", "I");
  QatDecompressor_uncompressedDirectBuf = (*env)->GetFieldID(env,clazz,
                                                             "uncompressedDirectBuf",
                                                             "Ljava/nio/Buffer;");
  QatDecompressor_directBufferSize = (*env)->GetFieldID(env, clazz,
                                                         "directBufferSize", "I");
}

JNIEXPORT jint JNICALL Java_org_apache_hadoop_io_compress_qat_QatDecompressor_decompressBytesDirect
(JNIEnv *env, jobject thisj){
  const unsigned char* compressed_bytes = NULL;
  unsigned char* uncompressed_bytes = NULL;
  unsigned int compressed_buf_len;
  int ret;
  // Get members of QatDecompressor
  jobject clazz = (*env)->GetStaticObjectField(env,thisj, QatDecompressor_clazz);
  jobject compressed_direct_buf = (*env)->GetObjectField(env,thisj, QatDecompressor_compressedDirectBuf);
  jint compressed_direct_buf_len = (*env)->GetIntField(env,thisj, QatDecompressor_compressedDirectBufLen);
  jobject uncompressed_direct_buf = (*env)->GetObjectField(env,thisj, QatDecompressor_uncompressedDirectBuf);
  unsigned int uncompressed_direct_buf_len = (*env)->GetIntField(env, thisj, QatDecompressor_directBufferSize);

  // Get the input direct buffer
  LOCK_CLASS(env, clazz, "QatDecompressor");
  compressed_bytes = (const unsigned char*)(*env)->GetDirectBufferAddress(env, compressed_direct_buf);
  UNLOCK_CLASS(env, clazz, "QatDecompressor");

  if (compressed_bytes == 0) {
    return (jint)0;
  }

  // Get the output direct buffer
  LOCK_CLASS(env, clazz, "QatDecompressor");
  uncompressed_bytes = (unsigned char *)(*env)->GetDirectBufferAddress(env, uncompressed_direct_buf);
  UNLOCK_CLASS(env, clazz, "QatDecompressor");

  if (uncompressed_bytes == 0) {
    return (jint)0;
  }

  compressed_buf_len = compressed_direct_buf_len;
  ret = dlsym_qzDecompress(&g_qzCompressSession, compressed_bytes, &compressed_buf_len,
        uncompressed_bytes, &uncompressed_direct_buf_len);
  if (ret != QZ_OK) {
    THROW(env, "java/lang/InternalError", "Could not decompress data, return " + ret);
  }

  (*env)->SetIntField(env, thisj, QatDecompressor_compressedDirectBufLen, 0);

  return (jint)uncompressed_direct_buf_len;
}

JNIEXPORT jobject JNICALL
Java_org_apache_hadoop_io_compress_qat_QatDecompressor_nativeAllocateBB(JNIEnv *env,
 jobject obj, jlong capacity, jboolean numa, jboolean force_pinned){
/*void *buf = dlsym_qzMalloc(capacity,0,1);
if (NULL == buf){
fprintf(stderr,"decompressor: DBB address is 0x%lx\n",(unsigned long)buf);
fflush(stderr);
}
  return (*env)->NewDirectByteBuffer(env, buf, capacity);*/
  return (*env)->NewDirectByteBuffer(env, dlsym_qzMalloc(capacity, numa, force_pinned), capacity);
}

#endif //define HADOOP_QAT_LIBRARY
