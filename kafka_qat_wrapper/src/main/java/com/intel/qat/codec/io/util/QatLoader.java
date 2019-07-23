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

package com.intel.qat.codec.io.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.intel.qat.codec.io.exception.QatErrorCode;
import com.intel.qat.codec.io.exception.QatIOException;
import com.intel.qat.codec.io.nativ.QatNative;

public class QatLoader {

  private static final int EOF = -1;
  private static final String LIB_NAME = "kafkaqatjni";
  private static final String kafkaQatNativeLibraryName = "lib" + LIB_NAME
      + ".so";
  private static final String PATH_PREFIX = "/";

  private static File nativeLibFile = null;

  private static QatNative qatApi;

  public static synchronized QatNative loadQatApi() throws QatIOException {
    if (qatApi == null) {
      synchronized (QatLoader.class) {
        if (qatApi == null) {
          loadNativeLibrary();
          qatApi = new QatNative();
        }
      }
    }
    return qatApi;
  }

  private static void loadNativeLibrary() throws QatIOException {
    try {
      nativeLibFile = findNativeLibrary();
      if (nativeLibFile != null) {
        // Load extracted or specified kafka qat native library.
        System.load(nativeLibFile.getAbsolutePath());
      } else {
        // Load pre installed kafka qat (in the path -Djava.library.path)
        System.loadLibrary(LIB_NAME);
      }
    } catch (Exception e) {
      throw new QatIOException(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY,
          e.getMessage());
    }
  }

  private static File findNativeLibrary() throws QatIOException {
    boolean hasNativeLib = QatLoader.class
        .getResource(PATH_PREFIX + kafkaQatNativeLibraryName) != null;
    if (!hasNativeLib) {
      // throw new QatIOException(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY,
      // "no native kafka qat library is found with name "
      // + kafkaQatNativeLibraryName);
      System.err.println(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY
          + "no native kafka qat library is found with name "
          + kafkaQatNativeLibraryName);
      return null;
    }

    // Temporary folder for the native lib. Use the value of
    // java.io.tmpdir
    File tempFolder = new File(System.getProperty("java.io.tmpdir"));
    if (!tempFolder.exists()) {
      // if created == false, it will fail eventually in the later part
      tempFolder.mkdirs();
    }

    // Extract and load a native library inside the jar file
    return extractLibraryFile(kafkaQatNativeLibraryName,
        tempFolder.getAbsolutePath());
  }

  private static File extractLibraryFile(String libraryFileName,
      String targetFolder) {

    // Attach UUID to the native library file to ensure multiple class loaders
    // can read the kafka qat codec multiple times.
    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("qat-kafka-%s-%s", uuid,
        libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);

    try {
      try (
          InputStream reader = QatLoader.class
              .getResourceAsStream(PATH_PREFIX + libraryFileName);
          FileOutputStream writer = new FileOutputStream(extractedLibFile)) {
        extractedLibFile.deleteOnExit();
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        while ((bytesRead = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, bytesRead);
        }
      }

      // Set executable (x) flag to enable Java to load the native library
      // Setting file flag may fail, but in this case another error will be
      // thrown in later phase
      extractedLibFile.setReadable(true);
      extractedLibFile.setWritable(true, true);
      extractedLibFile.setExecutable(true);

      try (
          InputStream nativeIn = QatLoader.class
              .getResourceAsStream("/" + libraryFileName);
          InputStream extractedLibIn = new FileInputStream(extractedLibFile)) {
        if (!contentEquals(nativeIn, extractedLibIn)) {
          throw new QatIOException(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY,
              String.format("Failed to write a native library file at %s",
                  extractedLibFile));
        }
      }

      return extractedLibFile;
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return null;
    }
  }

  public static boolean contentEquals(InputStream input1, InputStream input2)
      throws IOException {
    if (input1 == input2) {
      return true;
    }
    if (!(input1 instanceof BufferedInputStream)) {
      input1 = new BufferedInputStream(input1);
    }
    if (!(input2 instanceof BufferedInputStream)) {
      input2 = new BufferedInputStream(input2);
    }

    int ch = input1.read();
    while (EOF != ch) {
      final int ch2 = input2.read();
      if (ch != ch2) {
        return false;
      }
      ch = input1.read();
    }

    final int ch2 = input2.read();
    return ch2 == EOF;
  }
}
