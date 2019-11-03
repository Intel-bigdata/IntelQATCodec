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

package org.apache.orc.impl;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.apache.orc.CompressionCodec;

public class TestOrcQATCodec {
  final static int DIRECT_BUFFER_SIZE = 256 * 1024;

  @Test
  public void testNoOverflow() throws Exception {
    ByteBuffer in = ByteBuffer.allocate(10);
    ByteBuffer out = ByteBuffer.allocate(10);
    in.put(new byte[]{1,2,3,4,5,6,7,10});
    in.flip();
    CompressionCodec codec = new QATCodec(DIRECT_BUFFER_SIZE);
    assertEquals(false, codec.compress(in, out, null));
  }

  @Test
  public void testCorrupt() throws Exception {
    ByteBuffer in = ByteBuffer.allocate(10);
    ByteBuffer compressed = ByteBuffer.allocate(4096);
    ByteBuffer decompressed = ByteBuffer.allocate(10);
    in.put(new byte[]{127,-128,0,99,98,-1});
    in.flip();
    CompressionCodec codec = new QATCodec(DIRECT_BUFFER_SIZE);
    try {
      codec.compress(in, compressed, null);
      compressed.flip();
      codec.decompress(compressed, decompressed);
      //fail();
    } catch (IOException ioe) {
      // EXPECTED
    }
  }
}
