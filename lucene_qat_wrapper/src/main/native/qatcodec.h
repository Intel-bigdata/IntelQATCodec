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

/*  we currently use a number of macros defined in
 * com_intel_qat.h. These are:
 *   * UNIX
 *   * THROW
 *   * LOAD_DYNAMIC_SYMBOL
 *   * LOCK_CLASS
 *   * UNLOCK_CLASS
 *   * Probably at least one of the Windows-specific definitions too
 *
 * com_intel_qat.h also prevents us dropping config.h, as this file is
 * included by com_intel_qat.h. */
//#include "org_apache_hadoop.h"

#include "com_intel_qat.h"
#ifdef UNIX
#include <dlfcn.h>
#endif

#include <jni.h>
#include <qatzip.h>
#include <stddef.h>

#define LUCENE_QAT_LIBRARY "libqatzip.so"

extern __thread QzSession_T  g_qzCompressSession;
