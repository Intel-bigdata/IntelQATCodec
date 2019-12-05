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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intel.qat.codec.io.exception.QatErrorCode;
import com.intel.qat.codec.io.exception.QatIOException;

/**
 * QatLoader loads the kafka qat library if available in the jar, otherwise it
 * loads from the pre-installed kafka qat library.
 */
public final class QatLoader {
  private static final Logger LOG = LoggerFactory.getLogger(QatLoader.class);
  private static final int EOF = -1;
  private static final String LIB_NAME = "kafkaqatjni";
  private static final String KAFKA_QAT_NATIVE_LIBRARY_NAME = "lib" + LIB_NAME
      + ".so";
  private static final String PATH_PREFIX = "/";

  private static File nativeLibFile = null;

  private static boolean qatApiLoaded;

  private QatLoader() {
  }

  /**
   * loads the kafka qat library if available in the jar, otherwise loads from
   * the pre-installed kafka qat library.
   *
   * @throws QatIOException - unavoidable condition
   */
  public static synchronized void loadQatApi() throws QatIOException {
    if (!qatApiLoaded) {
      synchronized (QatLoader.class) {
        if (!qatApiLoaded) {
          loadNativeLibrary();
          qatApiLoaded = true;
        }
      }
    }
  }

  private static void loadNativeLibrary() throws QatIOException {
    try {
      nativeLibFile = findNativeLibrary();
      if (nativeLibFile != null) {
        // Load extracted or specified kafka qat native library.
        System.load(nativeLibFile.getAbsolutePath());
        LOG.info("Loaded the " + nativeLibFile.getAbsolutePath()
            + " library extracted from jar.");
      } else {
        // Load pre installed kafka qat (in the path -Djava.library.path)
        LOG.info("Could not load the " + LIB_NAME
            + "from the jar, loading the pre installed library.");
        System.loadLibrary(LIB_NAME);
      }
    } catch (Exception e) {
      throw new QatIOException(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY,
          e.getMessage());
    }
  }

  private static File findNativeLibrary() throws QatIOException {
    boolean hasNativeLib = QatLoader.class
        .getResource(PATH_PREFIX + KAFKA_QAT_NATIVE_LIBRARY_NAME) != null;
    if (!hasNativeLib) {
      LOG.warn(QatErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY
          + " no native kafka qat library is found with name "
          + KAFKA_QAT_NATIVE_LIBRARY_NAME);
      return null;
    }

    // Temporary folder for the native lib. Use the value of
    // java.io.tmpdir
    File tempFolder = new File(System.getProperty("java.io.tmpdir"));
    if (!tempFolder.exists()) {
      // if created == false, it will fail eventually in the later part
      if (!tempFolder.mkdirs()) {
        LOG.warn(
            "Failed to create the temp dir : " + tempFolder.getAbsolutePath());
      }
    }

    // Extract and load a native library inside the jar file
    return extractLibraryFile(KAFKA_QAT_NATIVE_LIBRARY_NAME,
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
      LOG.warn("Error while extracting the native library.", e);
      return null;
    }
  }

  private static boolean contentEquals(InputStream input1, InputStream input2)
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
