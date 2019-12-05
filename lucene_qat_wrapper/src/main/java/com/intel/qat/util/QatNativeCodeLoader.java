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

package com.intel.qat.util;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A helper to load the native qat code i.e. libqat.so.
 * This handles the fallback to either the bundled libhadoop-Linux-i386-32.so
 * or the default java implementations where appropriate.
 *  
 */

public class QatNativeCodeLoader {
  private static final Logger LOG = LoggerFactory.getLogger(QatNativeCodeLoader.class);
  //private static final Logger LOG = LogManager.getLogger(QatNativeCodeLoader.class);
  private static boolean nativeCodeLoaded = false;

  static {
    // Try to load native qat library and set fallback flag appropriately
    if(LOG.isDebugEnabled()) {
      LOG.debug("Trying to load the custom-built native-qat library...");
    }
    try {
      System.loadLibrary("qatcodec");
      LOG.debug("Loaded the native-qat library");
      nativeCodeLoaded = true;
    } catch (Throwable t) {
      // Ignore failure to load
      if(LOG.isDebugEnabled()) {
        LOG.debug("Failed to load native-qat with error: " + t);
        LOG.debug("java.library.path=" +
            System.getProperty("java.library.path"));
      }
    }

    if (!nativeCodeLoaded) {
      LOG.warn("Unable to load native-qat library for your platform... " +
               "using builtin-java classes where applicable");
    }
  }

  /**
   * Check if native-qat code is loaded for this platform.
   * 
   * @return <code>true</code> if native-qat is loaded, 
   *         else <code>false</code>
   */
  public static boolean isNativeCodeLoaded() {
    return nativeCodeLoaded;
  }

  /**
   * Returns true only if this build was compiled with support for qat.
   */
  public static native boolean buildSupportsQat();

  public static native String getLibraryName();
}
