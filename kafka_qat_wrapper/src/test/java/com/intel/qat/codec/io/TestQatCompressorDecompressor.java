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

package com.intel.qat.codec.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.intel.qat.codec.io.streams.QatInputStream;
import com.intel.qat.codec.io.streams.QatOutputStream;
import com.intel.qat.codec.io.util.DataBytesGenerator;

public class TestQatCompressorDecompressor {
  private static final byte[] text = "This is test message for compression"
      .getBytes();
  private File inputFile;
  private File compressedFile;
  private File decompressedFile;

  @Test
  public void testCompression() throws IOException {
    byte[] data = compressData();
    assertTrue("Compressed data should not be empty.", data.length > 0);
  }

  @Test
  public void testDecompression() throws IOException {
    byte[] b = new byte[100];
    int bytes;
    try (ByteArrayInputStream in = new ByteArrayInputStream(compressData())) {
      try (QatInputStream inStream = new QatInputStream(in)) {
        bytes = inStream.read(b);
      }
    }
    assertEquals("Decompressed data is not matching original data.",
        new String(b, 0, bytes), new String(text));
  }

  @Test
  public void testCompressionDecompression() throws Throwable {
    inputFile = generateRawDataFile();
    compressedFile = compressFile(inputFile);
    decompressedFile = decompressFile(compressedFile);
    boolean result = FileUtils.contentEquals(inputFile, decompressedFile);
    assertTrue("Decompressed file is not matching with original file.",
        result);
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(inputFile);
    FileUtils.deleteQuietly(compressedFile);
    FileUtils.deleteQuietly(decompressedFile);
  }

  private File decompressFile(File compressedFile)
      throws FileNotFoundException, IOException {
    File decompressedFile = new File("uncompressedfile");
    try (
        FileOutputStream fileOutputStream = new FileOutputStream(
            decompressedFile);
        BufferedOutputStream bufOut = new BufferedOutputStream(
            fileOutputStream);
        FileInputStream fileIn = new FileInputStream(compressedFile);
        QatInputStream qatInputStream = new QatInputStream(fileIn);) {
      byte[] bytes = new byte[100];
      while (qatInputStream.available() > 0) {
        int read = qatInputStream.read(bytes);
        bufOut.write(bytes, 0, read);
      }
    }
    return decompressedFile;
  }

  private File compressFile(File inputFile)
      throws FileNotFoundException, IOException {
    File compressedFile = new File("compressedfile");
    try (
        FileOutputStream fileOutputStream = new FileOutputStream(
            compressedFile);
        QatOutputStream outputStream = new QatOutputStream(fileOutputStream);
        FileInputStream fileIn = new FileInputStream(inputFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(
            fileIn)) {
      byte[] bytes = new byte[100];
      while (bufferedInputStream.available() > 0) {
        int read = bufferedInputStream.read(bytes);
        outputStream.write(bytes, 0, read);
      }
    }
    return compressedFile;
  }

  private File generateRawDataFile() throws IOException, FileNotFoundException {
    java.util.Random random = new java.util.Random();
    File inputFile = new File("testdata" + random.nextLong());
    try (FileOutputStream out = new FileOutputStream(inputFile)) {
      for (int i = 0; i < 1; i++) {
        out.write(DataBytesGenerator.get(10240));
      }
    }
    return inputFile;
  }

  private byte[] compressData() throws IOException {
    byte[] data;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        QatOutputStream outputStream = new QatOutputStream(out)) {
      outputStream.write(text);
      outputStream.flush();
      data = out.toByteArray();
    }
    return data;
  }
}
