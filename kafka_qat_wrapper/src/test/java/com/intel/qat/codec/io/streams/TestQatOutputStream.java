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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.intel.qat.codec.io.util.DataBytesGenerator;

/**
 * Test class to verify QatOutputStream.
 *
 */
public class TestQatOutputStream {

  @Test
  public void testWrite() throws IOException {
    byte[] input = DataBytesGenerator.get(10);
    byte[] data;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        QatOutputStream outputStream = new QatOutputStream(out)) {
      for (byte b : input) {
        outputStream.write(b);
      }
      outputStream.flush();
      data = out.toByteArray();
    }

    byte[] uncompressedData = new byte[input.length];
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(data))) {
      in.read(uncompressedData);
    }
    assertArrayEquals("Write data is not correct.", input, uncompressedData);
  }

  @Test
  public void testWriteWithParameters() throws IOException {
    byte[] input = DataBytesGenerator.get(10);
    byte[] data;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        QatOutputStream outputStream = new QatOutputStream(out)) {
      outputStream.write(input, 0, 5);
      outputStream.write(input, 5, 5);
      outputStream.flush();
      data = out.toByteArray();
    }

    byte[] uncompressedData = new byte[input.length];
    try (QatInputStream in = new QatInputStream(
        new ByteArrayInputStream(data))) {
      in.read(uncompressedData);
    }
    assertArrayEquals("Write data is not correct.", input, uncompressedData);
  }

  @Test
  public void testClose() throws IOException {
    final AtomicBoolean closeInvoked = new AtomicBoolean();
    ByteArrayOutputStream out = new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        closeInvoked.set(true);
      }
    };
    try (QatOutputStream outputStream = new QatOutputStream(out)) {
    }
    assertTrue("close should be invoked.", closeInvoked.get());
  }

  @Test
  public void testFlush() throws IOException {
    final AtomicBoolean flushInvoked = new AtomicBoolean();
    ByteArrayOutputStream out = new ByteArrayOutputStream() {
      @Override
      public void flush() throws IOException {
        super.flush();
        flushInvoked.set(true);
      }
    };
    try (QatOutputStream outputStream = new QatOutputStream(out)) {
    }
    assertTrue("flush should be invoked.", flushInvoked.get());

    AtomicBoolean flushInvokedExp = new AtomicBoolean();
    out = new ByteArrayOutputStream() {
      @Override
      public void flush() throws IOException {
        super.flush();
        flushInvokedExp.set(true);
      }
    };
    try (QatOutputStream outputStream = new QatOutputStream(out)) {
    }
    assertTrue("flush should be invoked.", flushInvokedExp.get());
  }
}
