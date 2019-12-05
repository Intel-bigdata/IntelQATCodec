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

package com.intel.qat.codec.io.nativ;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.intel.qat.codec.io.jni.QatNative;
import com.intel.qat.codec.io.util.DataBytesGenerator;

/**
 * Test class to verify JNI invocations.
 */
public class TestQatCodecJNI {

  @Test
  public void testInit() {
    try {
      QatNative.init();
    } catch (Throwable t) {
      fail("Init should not throw any error.");
    }
  }

  @Test
  public void testAllocNativeBuffer() throws Exception {
    QatNative.init();
    Object buffer0 = QatNative.allocNativeBuffer(1024, 64);
    assertEquals(1024, ((ByteBuffer) buffer0).capacity());
    Object buffer1 = QatNative.allocNativeBuffer(32 * 1024, 64);
    assertEquals(32 * 1024, ((ByteBuffer) buffer1).capacity());
  }

  @Test
  public void testCreateCompressContext() throws Exception {
    long compressContext = QatNative.createCompressContext(1);
    assertNotEquals(0, compressContext);
    long compressContext1 = QatNative.createCompressContext(1024);
    assertNotEquals(0, compressContext1);
    long compressContext2 = QatNative.createCompressContext(4);
    assertNotEquals(0, compressContext2);
  }

  @Test
  public void testGetLibraryName() throws Exception {
    String name = QatNative.getLibraryName();
    assertNotNull("Should return library name.", name);
  }

  @Test
  public void testCompress() throws Exception {
    QatNative.init();
    final int capacity = 1024 * 1024;
    final int align = 64;
    ByteBuffer rawDataBuffer = (ByteBuffer) QatNative
        .allocNativeBuffer(capacity, align);
    ByteBuffer compressDataBuffer = (ByteBuffer) QatNative
        .allocNativeBuffer(capacity, align);

    ByteBuffer uncompressDataBuffer = (ByteBuffer) QatNative
        .allocNativeBuffer(capacity, align);
    byte[] bs = DataBytesGenerator.get(capacity);
    rawDataBuffer.put(bs);

    long compressContext = QatNative.createCompressContext(1);
    int compress = QatNative.compress(compressContext, rawDataBuffer, 0,
        bs.length, compressDataBuffer, 0, compressDataBuffer.capacity());

    int decompress = QatNative.decompress(compressDataBuffer, 0, compress,
        uncompressDataBuffer, 0, uncompressDataBuffer.capacity());
    byte[] dst = new byte[decompress];
    uncompressDataBuffer.get(dst);
    assertArrayEquals("Raw data and uncompressed data are not matching.", bs,
        dst);
  }
}
