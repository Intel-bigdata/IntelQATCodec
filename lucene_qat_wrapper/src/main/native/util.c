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

#include <jni.h>
#include <stdio.h>
#include <string.h>
/*
#include "org_apache_hadoop_util_QatNativeCodeLoaderTest.h"
#include "com_intel_qat_util_QatNativeCodeLoader.h"
*/
/*
 * Class:     com_intel_qat_util_QatNativeCodeLoaderTest
 * Method:    buildSupportsQat
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_qat_util_QatNativeCodeLoaderTest_buildSupportsQat
  (JNIEnv *env, jclass cl){
	return 1;
 }

/*
 * Class:     com_intel_qat_util_QatNativeCodeLoaderTest
 * Method:    getLibraryName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_intel_qat_util_QatNativeCodeLoaderTest_getLibraryName
  (JNIEnv *env, jclass cl) {
	jstring name;
//	mystring = (*env)->NewStringUTF(env,"QAT Compressor");
        name = (*env)->NewStringUTF(env, "kettle");
	return(name);
}
  
/*
 * Class:     com_intel_qat_util_QatNativeCodeLoader
 * Method:    buildSupportsQat
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_qat_util_QatNativeCodeLoader_buildSupportsQat
  (JNIEnv *env, jclass cl){
	return 1;
}

/*
 * Class:     com_intel_qat_util_QatNativeCodeLoader
 * Method:    getLibraryName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_intel_qat_util_QatNativeCodeLoader_getLibraryName
  (JNIEnv *env, jclass cl) {
	jstring name;
//	mystring = (*env)->NewStringUTF(env,"QAT Compressor");
        name = (*env)->NewStringUTF(env, "kettle");
	return(name);
}
