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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class TestQat {

  @Test
  public void testQatCompress() throws IOException {
    byte[] input = "This is test message".getBytes();
    byte[] compressed = new byte[input.length * 10];
    int compressedLength = Qat.compress(input, 0, input.length, compressed, 0);
    assertTrue("Compressed length should be more than 0", compressedLength > 0);
  }

  @Test
  public void testQatDecompress() throws IOException {
    byte[] input = "This is test message".getBytes();
    byte[] compressed = new byte[input.length * 10];
    int compressedLength = Qat.compress(input, 0, input.length, compressed, 0);

    byte[] uncompressedData = new byte[input.length];
    int uncompress = Qat.uncompress(compressed, 0, compressedLength,
        uncompressedData, 0);
    assertEquals("Original data length shoud be same as uncompressed data size",
        uncompress, input.length);
    assertEquals("Original and uncompressed data is not equal.",
        new String(input), new String(uncompressedData));
  }

  @Test
  public void testQatCompressDecompress() throws IOException {
    byte[] input = DataBytesGenerator.get(102400);
    byte[] compressed = new byte[input.length * 10];
    int compressedLength = Qat.compress(input, 0, input.length, compressed, 0);

    byte[] uncompressedData = new byte[input.length];
    int uncompress = Qat.uncompress(compressed, 0, compressedLength,
        uncompressedData, 0);
    assertEquals(
        "Original data length should be same as uncompressed data size",
        uncompress, input.length);
    assertArrayEquals("Original and uncompressed data is not equal.", input,
        uncompressedData);
    System.out.println("End of the test");
  }

  @Test
  public void testMaxCompressedLength() {
    int maxCompressedLength = Qat.maxCompressedLength(3 * 1024);
    assertEquals("maxCompressedLength is different, ", 3536,
        maxCompressedLength);
  }
}
