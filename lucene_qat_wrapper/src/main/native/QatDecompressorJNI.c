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

#if defined LUCENE_QAT_LIBRARY

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>


#include "com_intel_qat_jni_QatDecompressorJNI.h"


static jfieldID QatDecompressorJNI_clazz;
static jfieldID QatDecompressorJNI_compressedDirectBuf;
static jfieldID QatDecompressorJNI_compressedDirectBufLen;
static jfieldID QatDecompressorJNI_uncompressedDirectBuf;
static jfieldID QatDecompressorJNI_directBufferSize;

#define qaePinnedMemAlloc(x, y)  qaeMemAllocNUMA((x), (y), 8)
#define qaePinnedMemFree(x)      qaeMemFreeNUMA((void **)&(x))

/*__thread QzSession_T  g_qzDecompressSession = {
    .internal = NULL,
};*/

#ifdef UNIX
static int (*dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
    unsigned char* (*dlsym_qzMalloc)(int sz, int numa, int force_pinned);
    int (*dlsym_qzGetDefaults)(QzSessionParams_T *defaults);
    int (*dlsym_qzSetDefaults)(QzSessionParams_T *defaults);
#endif

#ifdef WINDOWS
typedef int (__cdecl *__dlsym_qzDecompress)(QzSession_T *sess, const unsigned char* src,
    unsigned int* compressed_buf_len, unsigned char* dest,
    unsigned int* uncompressed_buffer_len);
static __dlsym_qzDecompress dlsym_qzDecompress;
#endif

JNIEXPORT void JNICALL Java_com_intel_qat_jni_QatDecompressorJNI_initIDs
(JNIEnv *env, jclass clazz){
QzSession_T  g_qzDecompressSession = {
    .internal = NULL,
};
  // Load libqatzip.so
#ifdef UNIX
  void *libqatzip = dlopen("libqatzip.so", RTLD_LAZY | RTLD_GLOBAL);
  if (!libqatzip) {
    char msg[1000];
    snprintf(msg, sizeof(msg), "%s (%s)!", "Cannot load " LUCENE_QAT_LIBRARY, dlerror());
    THROW(env, "java/lang/UnsatisfiedLinkError", msg);
    return;
  }
#endif

#ifdef WINDOWS
  HMODULE libqatzip = LoadLibrary(LUCENE_QAT_LIBRARY);
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
  LOAD_DYNAMIC_SYMBOL(dlsym_qzGetDefaults, env, libqatzip, "qzGetDefaults");
  LOAD_DYNAMIC_SYMBOL(dlsym_qzSetDefaults, env, libqatzip, "qzSetDefaults");
#endif

#ifdef WINDOWS
  LOAD_DYNAMIC_SYMBOL(__dlsym_qatzip_uncompress, dlsym_qzDecompress, env, libqatzip, "qzDecompress");
#endif

  fprintf(stderr, "-------> decompression level is feeeeeeeffffffffeeeee20200109-20200219-20200225, tid is %d\n",syscall(__NR_gettid));
  fflush(stderr);
  QatDecompressorJNI_clazz = (*env)->GetStaticFieldID(env, clazz, "clazz",
                                                   "Ljava/lang/Class;");
  QatDecompressorJNI_compressedDirectBuf = (*env)->GetFieldID(env,clazz,
                                                           "compressedDirectBuf",
                                                           "Ljava/nio/Buffer;");
  QatDecompressorJNI_compressedDirectBufLen = (*env)->GetFieldID(env,clazz,
                                                              "compressedDirectBufLen", "I");
  QatDecompressorJNI_uncompressedDirectBuf = (*env)->GetFieldID(env,clazz,
                                                             "uncompressedDirectBuf",
                                                             "Ljava/nio/Buffer;");
  QatDecompressorJNI_directBufferSize = (*env)->GetFieldID(env, clazz,
                                                         "directBufferSize", "I");
}

JNIEXPORT jint JNICALL Java_com_intel_qat_jni_QatDecompressorJNI_decompressBytesDirect
(JNIEnv *env, jobject thisj){
/*
   g_qzDecompressSession = {
        .internal = NULL,
   };
*/
    QzSession_T  g_qzDecompressSession = {
    .internal = NULL,
    };
  const unsigned char* compressed_bytes = NULL;
  unsigned char* uncompressed_bytes = NULL;
  unsigned int compressed_buf_len;
  int ret;

  FILE *p;
  //p = fopen("/home/sparkuser/Downloads/QatDecompressorJNI_decompressBytesDirect.txt","wb+");
  //p = fopen("/home/sparkuser/Downloads/QatDecompressorJNI_decompressBytesDirect.txt","w+");
  p = fopen("/tmp/QatDecompressorJNI_decompressBytesDirect1.txt","a+");
  fprintf(stderr, " tid is %d\n", syscall(__NR_gettid));
  fprintf(p, " 20200219-20200225 , tid is %d\n", syscall(__NR_gettid));
  if(p!=NULL){
  fprintf(stderr,"the file have open \n");
  fflush(stderr);
  }
  else{
  fprintf(stderr,"the file not open \n");
  fflush(stderr);
  }

  fprintf(stderr,"----->out of file \n");

  // Get members of QatDecompressorJNI
  jobject clazz = (*env)->GetStaticObjectField(env,thisj, QatDecompressorJNI_clazz);
  jobject compressed_direct_buf = (*env)->GetObjectField(env,thisj, QatDecompressorJNI_compressedDirectBuf);
  jint compressed_direct_buf_len = (*env)->GetIntField(env,thisj, QatDecompressorJNI_compressedDirectBufLen);
  jobject uncompressed_direct_buf = (*env)->GetObjectField(env,thisj, QatDecompressorJNI_uncompressedDirectBuf);
  unsigned int uncompressed_direct_buf_len = (*env)->GetIntField(env, thisj, QatDecompressorJNI_directBufferSize);


   fprintf(p,"Before decompress, the compressed_direct_buf_len is %d \n",compressed_direct_buf_len);
   fprintf(p,"Before decompress, the uncompressed_direct_buf_len is %d \n",uncompressed_direct_buf_len);


  // Get the input direct buffer
  LOCK_CLASS(env, clazz, "QatDecompressorJNI");
  compressed_bytes = (const unsigned char*)(*env)->GetDirectBufferAddress(env, compressed_direct_buf);
  UNLOCK_CLASS(env, clazz, "QatDecompressorJNI");

/*  fprintf(p,"the compressed_bytes get from the input direct buffer : \n");
 // fputs(compressed_bytes,p);

    int i;
    for(i = 0; i < compressed_direct_buf_len; i++){
        fprintf(p,"%02x , ",compressed_bytes[i]);
        fprintf(stderr,"%02x , ",compressed_bytes[i]);
    }*/

  if (compressed_bytes == 0) {
    return (jint)0;
  }

  // Get the output direct buffer
  LOCK_CLASS(env, clazz, "QatDecompressorJNI");
  uncompressed_bytes = (unsigned char *)(*env)->GetDirectBufferAddress(env, uncompressed_direct_buf);
  UNLOCK_CLASS(env, clazz, "QatDecompressorJNI");

  //fprintf(p,"the uncompressed_bytes get from the input direct buffer : \n");
  //fputs(uncompressed_bytes,p);
  //fprintf(p,"\n");

 /* for(i = 0; i < uncompressed_direct_buf_len; i++){
      fprintf(p,"%02x , ",uncompressed_bytes[i]);
      fprintf(stderr,"%02x , ",uncompressed_bytes[i]);
  }*/

  if (uncompressed_bytes == 0) {
    return (jint)0;
  }


  compressed_buf_len = compressed_direct_buf_len;
  fprintf(p, "\n ----------------------> the compressed_buf_len before qzDecompress is  %d : \n", compressed_buf_len);
 /*fprintf(p,"the compressed_bytes before qzDecompress : \n");
 int i =0;
  for(i = 0; i < compressed_direct_buf_len; i++){
      fprintf(p,"%02x , ",compressed_bytes[i]);
      fprintf(stderr,"%02x , ",compressed_bytes[i]);
  }*/


  ret = dlsym_qzDecompress(&g_qzDecompressSession, compressed_bytes, &compressed_buf_len,
        uncompressed_bytes, &uncompressed_direct_buf_len);

/*const unsigned char compressed_bytes1[38] = {0x2f , 0x8b , 0x08 , 0x04 , 00 , 00 , 00 , 00 , 00 , 0xff , 0x0c , 00 , 0x51 , 0x5a , 0x08 , 00 , 00 , 00 , 00 , 00 , 00 , 00 , 00 , 00 , 0x63 , 0x62 , 0x66 , 01 , 00 , 0xa6 , 0x9a , 0x85 , 0xd0 , 03 , 00 , 00 , 00 , 00};
 jint compressed_direct_buf_len1=38;
 compressed_buf_len = compressed_direct_buf_len1;

 fprintf(p, "\n ----------------------> the compressed_buf_len before qzDecompress is  %d : \n", compressed_buf_len);

 fprintf(p, "\n ----------------------> the compressed_bytes1 before qzDecompress : \n");
  //fputs(compressed_bytes,p);
    for(i = 0; i < 38; i++){
        fprintf(p,"%02x , ",compressed_bytes1[i]);
        fprintf(stderr,"%02x , ",compressed_bytes1[i]);
    }

 ret = dlsym_qzDecompress(&g_qzCompressSession, compressed_bytes1, &compressed_buf_len,
        uncompressed_bytes, &uncompressed_direct_buf_len);

 fprintf(p,"\n the compressed_buf_len after qzDecompress %d: \n",compressed_buf_len);
 fprintf(p,"\n the uncompressed_direct_buf_len after qzDecompress %d : \n", uncompressed_direct_buf_len);

 fprintf(p, "\n ------------------->the compressed_bytes1 after qzDecompress : \n");
   for(i = 0; i < 38; i++){
        fprintf(p,"%02x , ",compressed_bytes1[i]);
        fprintf(stderr,"%02x , ",compressed_bytes1[i]);
    }
 */

  fprintf(p,"\n the compressed_buf_len after qzDecompress %d: \n",compressed_buf_len);

  fprintf(p,"\n the uncompressed_direct_buf_len after qzDecompress %d : \n", uncompressed_direct_buf_len);


 /* fprintf(p, "\n the compressed_bytes after qzDecompress : \n");
  //fputs(compressed_bytes,p);
    for(i = 0; i < compressed_direct_buf_len; i++){
        fprintf(p,"%02x , ",compressed_bytes[i]);
        fprintf(stderr,"%02x , ",compressed_bytes[i]);
    }


  fprintf(p,"\n the uncompressed_bytes after qaDecompress : \n");
  //fputs(uncompressed_bytes,p);
    for(i = 0; i < uncompressed_direct_buf_len; i++){
        fprintf(p,"%02x , ",uncompressed_bytes[i]);
        fprintf(stderr,"%02x , ",uncompressed_bytes[i]);
    }*/


  fprintf(p,"\n the result of the qzDecompress is %d \n", ret);

  fprintf(stderr,"\n the result of the qzDecompress is %d \n", ret);

  //   fwrite(in,srcLen,1,p);
     //fprintf(p,"%d",out);
     fclose(p);

  if (ret != QZ_OK) {
    THROW(env, "java/lang/InternalError", "Could not decompress data, return " + ret);
  }

  (*env)->SetIntField(env, thisj, QatDecompressorJNI_compressedDirectBufLen, 0);

  return (jint)uncompressed_direct_buf_len;
}

JNIEXPORT jobject JNICALL
Java_com_intel_qat_jni_QatDecompressorJNI_nativeAllocateBB(JNIEnv *env,
 jobject obj, jlong capacity, jboolean numa, jboolean force_pinned){
/*void *buf = dlsym_qzMalloc(capacity,0,1);
if (NULL == buf){
fprintf(stderr,"decompressor: DBB address is 0x%lx\n",(unsigned long)buf);
fflush(stderr);
}
  return (*env)->NewDirectByteBuffer(env, buf, capacity);*/
  return (*env)->NewDirectByteBuffer(env, dlsym_qzMalloc(capacity, numa, force_pinned), capacity);
}

#endif //define LUCENE_QAT_LIBRARY
