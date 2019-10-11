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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.X11.XSystemTrayPeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NativeCodeLoader {


  private static final Logger LOG = LoggerFactory.getLogger(NativeCodeLoader.class);
  private static final String LIBRARY_NAME = "QatCodecEs";
  //private static final String LIBRARY_NAME = "libQatCodecEs";
  private static boolean nativeCodeLoaded = false;

  static {
    // Try to load native library
    System.out.println("-----------------------------------Trying to load the native library...-----------------------------------------");
    LOG.info("Trying to load the native library...");

    load();
  }

  private enum OS {
    WINDOWS("Windows", "dll"), LINUX("Linux", "so"), MAC("Mac", "dylib");
    public final String name, libExtension;

    private OS(String name, String libExtension) {
      this.name = name;
      this.libExtension = libExtension;
    }
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
      //System.out.println("------------------------------osName---------------------------------");
      //System.out.println(OS.LINUX);
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
      System.out.println("----------------------------is nativeCodeLoaded------------------------------------");
      return;
    }

    // Try to load library from the java.library.
    try {
      System.out.println("-----------------------------TRY TO LOAD LIB FROM< /lib64-----------------------------------");
      System.loadLibrary(LIBRARY_NAME);
      System.out.println("----------------------------LOAD LIBRARY  TEST-***/lib64-0918 ------------------------------------------------");
      nativeCodeLoaded = true;
      LOG.info("Loaded native lib" + LIBRARY_NAME + "." + os().libExtension
          + " from the system library path");
      System.out.println("---------------------------------------------Loaded native lib" + LIBRARY_NAME + "." +
              os().libExtension + " from the system library path-----------------------------------------------------------");
      return;
    } catch(Exception e){
      // Doesn't exist, so proceed to loading bundled library.
      System.out.println("-------WRONG PROBLEMS REASON--------------------------------------"+ e +"-------------------");
    }

    System.out.println("----------------------------LOADLIBRARY  TEST- FROM JAR 0918------------------------------------------------");
    String resourceName = resourceName();
    System.out.println("resourceName is " + resourceName + "-------------------------------");// /com/intel/qat/native/lib/Linux/amd64/libQatCodecEs.so-
    InputStream is = NativeCodeLoader.class.getResourceAsStream(resourceName);
    if (is == null) {
      System.out.println("--------------------------------------Unsupported OS/arch, cannot find "
              + resourceName + ". Please try building from source.--------------------------");

      throw new UnsupportedOperationException("Unsupported OS/arch, cannot find "
          + resourceName + ". Please try building from source.");
    }
    File tempLib;
    try {
      System.out.println("-----------------LIBRARY NAME -----" + LIBRARY_NAME + "-----------------");
      tempLib = File.createTempFile("lib" + LIBRARY_NAME, "." + os().libExtension);
      System.out.println("-------------------------tempLib is "+ tempLib + "-------------------------------");//tempLib is ./temp/libQatCodecEs4362047548148073749.so
      // copy to tempLib
      FileOutputStream out = new FileOutputStream(tempLib);
      System.out.println("-------------------------out is "+ out + "-------------------------------");//out is java.io.FileOutputStream@bd51972
      try {
        byte[] buf = new byte[4096];
        while (true) {
          int read = is.read(buf);
          if (read == -1) {
            break;
          }
          out.write(buf, 0, read);
        }
        System.out.println("-------------------------After read the out is "+ out + "-------------------------------");//After read the out is java.io.FileOutputStream@bd51972-
        try {
          out.close();
          out = null;
        } catch (IOException e) {
          // ignore
          System.out.println("Error when closing the out "+ e + "-------------------------------------------");
        }
        try {
          // tempLib.getAbsolutePath()  /home/sparkuser/Downloads/elasticsearch/server/build/testrun/test/./temp/libQatCodecEs4362047548148073749.so
          System.out.println("-----------------tempLib.getAbsolutePath() is " + tempLib.getAbsolutePath() +"  and start load ------------------------");
          System.load(tempLib.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
          System.out.println("--------------LOAD ERROR "+ e+"---------------------------");
          System.out.println("-----------------------------------------Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension
                  + " from the embedded jar package-----------------------------------------------------");

          LOG.info("Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension
              + " from the embedded jar package");
          throw e;
        }
        nativeCodeLoaded = true;
        System.out.println("-------------------------------------------------Loaded native lib" + LIBRARY_NAME + "." + os().libExtension
                + " from the embedded jar package--------------------------------------------------");


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
      System.out.println("--------------------------------------Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension +
              "------------------------------------------------");



      LOG.error("Failed to load native lib" + LIBRARY_NAME + "." + os().libExtension);
      throw new ExceptionInInitializerError(new Throwable("Cannot unpack " + LIBRARY_NAME, e));
    }
  }


 /* public static void main(String[] args){
    System.out.println("hhhhhhhhhddd");

  }*/
}
