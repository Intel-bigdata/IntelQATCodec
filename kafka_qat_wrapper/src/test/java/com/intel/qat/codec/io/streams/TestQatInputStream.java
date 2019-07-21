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

package com.intel.qat.codec.io.streams;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.intel.qat.codec.io.exception.QatIOException;
import com.intel.qat.codec.io.util.DataBytesGenerator;

public class TestQatInputStream {

  @Test
  public void testEmptyInputStream() throws IOException {
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(new byte[0]))) {
      fail("QatIOException should be thrown.");
    } catch (QatIOException e) {
      assertEquals("[EMPTY_INPUT] Cannot decompress empty stream",
          e.getMessage());
    }
  }

  @Test
  public void testInputStreamWithNoHeader() throws IOException {
    byte[] input = DataBytesGenerator.get(10);
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(compressData(input)))) {
      byte[] uncompressedData = new byte[input.length];
      in.read(uncompressedData);
      assertArrayEquals(
          "Input should match with uncompressed data without header.", input,
          uncompressedData);
    }
  }

  @Test
  public void testRead() throws Exception {
    byte[] input = DataBytesGenerator.get(10);
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(compressData(input)))) {
      for (byte b : input) {
        if (b != in.read()) {
          fail("Input data is not matching with read.");
        }
      }
    }
  }

  @Test
  public void testReadWithParams() throws Exception {
    byte[] input = DataBytesGenerator.get(10);
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(compressData(input)))) {
      int available = in.available();
      byte[] data = new byte[available - 5];
      int read = in.read(data, 0, data.length);
      assertArrayEquals("First part is not matching.",
          Arrays.copyOfRange(input, 0, read), data);
      byte[] data1 = new byte[5];
      int available1 = in.available();
      in.read(data1, 0, available1);
      assertArrayEquals("Second part is not matching.",
          Arrays.copyOfRange(input, 5, input.length), data1);
    }
  }

  @Test
  public void testAvailable() throws Exception {
    byte[] input = DataBytesGenerator.get(10);
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(compressData(input)))) {
      int available = in.available();
      byte[] data = new byte[available];
      in.read(data);
      available = in.available();
      assertEquals("No bytes should be available.", 0, available);
    }
  }

  @Test
  public void testClose() throws Exception {
    byte[] input = DataBytesGenerator.get(10);
    final AtomicBoolean closeInvoked = new AtomicBoolean();
    ByteArrayInputStream byteInStream = new ByteArrayInputStream(
        compressData(input)) {
      @Override
      public void close() throws IOException {
        super.close();
        closeInvoked.set(true);
      }
    };
    try (QatInputStream in = new QatInputStream(byteInStream)) {
    }
    assertTrue("close should be invoked.", closeInvoked.get());
  }

  private byte[] compressData(byte[] input) throws IOException {
    byte[] data;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        QatOutputStream outputStream = new QatOutputStream(out)) {
      outputStream.write(input);
      outputStream.flush();
      data = out.toByteArray();
    }
    return data;
  }
}
