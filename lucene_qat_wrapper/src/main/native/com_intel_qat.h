/**
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

/**
 * This file includes some common utilities
 * for all native code used in hadoop.
 */

#if !defined COM_INTEL_QAT_H
#define COM_INTEL_QAT_H

#if defined(_WIN32)
#undef UNIX
#define WINDOWS
#else
#undef WINDOWS
#define UNIX
#endif

/* A helper macro to 'throw' a java exception. */
#define THROW(env, exception_name, message) \
  { \
	jclass ecls = (*env)->FindClass(env, exception_name); \
	if (ecls) { \
	  (*env)->ThrowNew(env, ecls, message); \
	  (*env)->DeleteLocalRef(env, ecls); \
	} \
  }

/* Helper macro to return if an exception is pending */
#define PASS_EXCEPTIONS(env) \
  { \
    if ((*env)->ExceptionCheck(env)) return; \
  }

#define PASS_EXCEPTIONS_GOTO(env, target) \
  { \
    if ((*env)->ExceptionCheck(env)) goto target; \
  }

#define PASS_EXCEPTIONS_RET(env, ret) \
  { \
    if ((*env)->ExceptionCheck(env)) return (ret); \
  }

/**
 * Unix definitions
 */
#ifdef UNIX
#include <config.h>
#include <dlfcn.h>
#include <jni.h>

/**
 * A helper function to dlsym a 'symbol' from a given library-handle.
 *
 * @param env jni handle to report contingencies.
 * @param handle handle to the dlopen'ed library.
 * @param symbol symbol to load.
 * @return returns the address where the symbol is loaded in memory,
 *         <code>NULL</code> on error.
 */
static __attribute__ ((unused))
void *do_dlsym(JNIEnv *env, void *handle, const char *symbol) {
  if (!env) {
    return NULL;
  }
  if (!handle || !symbol) {
  	THROW(env, "java/lang/InternalError", NULL);
  	return NULL;
  }
  char *error = NULL;
  void *func_ptr = dlsym(handle, symbol);
  if ((error = dlerror()) != NULL) {
  	THROW(env, "java/lang/UnsatisfiedLinkError", symbol);
  	return NULL;
  }
  return func_ptr;
}

/* A helper macro to dlsym the requisite dynamic symbol and bail-out on error. */
#define LOAD_DYNAMIC_SYMBOL(func_ptr, env, handle, symbol) \
  if ((func_ptr = do_dlsym(env, handle, symbol)) == NULL) { \
    return; \
  }
#endif
// Unix part end

#define LOCK_CLASS(env, clazz, classname) \
  if ((*env)->MonitorEnter(env, clazz) != 0) { \
    char exception_msg[128]; \
    snprintf(exception_msg, 128, "Failed to lock %s", classname); \
    THROW(env, "java/lang/InternalError", exception_msg); \
  }

#define UNLOCK_CLASS(env, clazz, classname) \
  if ((*env)->MonitorExit(env, clazz) != 0) { \
    char exception_msg[128]; \
    snprintf(exception_msg, 128, "Failed to unlock %s", classname); \
    THROW(env, "java/lang/InternalError", exception_msg); \
  }

#define RETRY_ON_EINTR(ret, expr) do { \
  ret = expr; \
} while ((ret == -1) && (errno == EINTR));

#endif

//vim: sw=2: ts=2: et
