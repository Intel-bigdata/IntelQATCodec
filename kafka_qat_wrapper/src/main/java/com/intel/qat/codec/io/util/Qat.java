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

import java.io.IOException;

import com.intel.qat.codec.io.nativ.QatNative;

public class Qat {

  private static QatNative impl;

  static {
    try {
//      impl = new QatNative();
      impl = QatLoader.loadQatApi();
      impl.init();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static int compress(byte[] input, int inputOffset, int inputLength,
      byte[] compressed, int outputOffset) throws IOException {
    return impl.compress(input, inputOffset, inputLength, compressed,
        outputOffset);
  }

  public static int uncompress(byte[] compressed, int inputOffset,
      int inputLength, byte[] decompressed, int outputOffset)
      throws IOException {
    return impl.decompress(compressed, inputOffset, inputLength, decompressed,
        outputOffset);
  }

  // public static int uncompressedLength(byte[] compressed, int offset, int
  // len) throws IOException {
  // return impl.uncompressedLength(compressed, offset, len);
  // }

  public static int maxCompressedLength(int blockSize) {
    return impl.maxCompressedLength(blockSize);
  }

  public static void arraycopy(Object src, int srcPos, Object dest, int destPos,
      int length) {
    System.arraycopy(src, srcPos, dest, destPos, length);
  }
}
