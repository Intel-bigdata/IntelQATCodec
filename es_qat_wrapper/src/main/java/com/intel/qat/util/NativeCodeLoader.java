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

package com.intel.qat.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NativeCodeLoader {

    private static final Logger LOG = LogManager.getLogger(NativeCodeLoader.class);

    private static final String LIBRARY_NAME = "QatCodecEs";
    private static boolean nativeCodeLoaded = false;

    static {
        // Try to load native library
        LOG.info("Trying to load the native library...");
        load();
    }

    private static String arch() {
        String archName = System.getProperty("os.arch");
        if (archName.contains("amd64")) {
            return archName;
        } else {
            throw new UnsupportedOperationException("Unsupported arch: "
                    + archName);
        }
    }

    private static OS os() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            return OS.LINUX;
        } else if (osName.contains("Mac")) {
            return OS.MAC;
        } else if (osName.contains("Windows")) {
            return OS.WINDOWS;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
    }

    private static String resourceName() {
        OS os = os();

        return "/com/intel/qat/native/lib/" + os.name + "/" + arch()
                + "/lib" + LIBRARY_NAME + "." + os.libExtension;
    }

    public static synchronized boolean isNativeCodeLoaded() {
        return nativeCodeLoaded;
    }

    public static synchronized void load() {

        if (nativeCodeLoaded) {
            return;
        }

        // Try to load library from the java.library.
        try {
            System.loadLibrary(LIBRARY_NAME);
            nativeCodeLoaded = true;
            LOG.info("Loaded native lib" + LIBRARY_NAME + "." + os().libExtension
                    + " from the system library path");
            return;
        } catch (UnsatisfiedLinkError ex) {
            // Doesn't exist, so proceed to loading bundled library.
        }

        String resourceName = resourceName();

        InputStream is = NativeCodeLoader.class.getResourceAsStream(resourceName);
        if (is == null) {
            throw new UnsupportedOperationException("Unsupported OS/arch, cannot find "
                    + resourceName + ". Please try building from source.");
        }
        File tempLib;
        try {
            tempLib = File.createTempFile("lib" + LIBRARY_NAME, "." + os().libExtension);
            // copy to tempLib
            FileOutputStream out = new FileOutputStream(tempLib);
            try {
                byte[] buf = new byte[4096];
                while (true) {
                    int read = is.read(buf);
                    if (read == -1) {
                        break;
                    }
                    out.write(buf, 0, read);
                }
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    // ignore
                }
                try {
                    System.load(tempLib.getAbsolutePath());
                } catch (UnsatisfiedLinkError e) {
                    LOG.info("Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension
                            + " from the embedded jar package");
                    throw e;
                }
                nativeCodeLoaded = true;
                LOG.info("Loaded native lib" + LIBRARY_NAME + "." + os().libExtension
                        + " from the embedded jar package");
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
                if (tempLib != null && tempLib.exists()) {
                    if (!nativeCodeLoaded) {
                        tempLib.delete();
                    } else {
                        // try to delete on exit, does it work on Windows?
                        tempLib.deleteOnExit();
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension);
            throw new ExceptionInInitializerError(new Throwable("Cannot unpack " + LIBRARY_NAME, e));
        }
    }

    private enum OS {
        WINDOWS("Windows", "dll"), LINUX("Linux", "so"), MAC("Mac", "dylib");
        public final String name, libExtension;

        OS(String name, String libExtension) {
            this.name = name;
            this.libExtension = libExtension;
        }
    }

}
