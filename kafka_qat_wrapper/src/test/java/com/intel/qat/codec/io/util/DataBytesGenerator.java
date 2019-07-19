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

import java.lang.reflect.Array;
import java.util.Random;

public final class DataBytesGenerator {
  private DataBytesGenerator() {
  }

  private static final byte[] CACHE = new byte[] { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5,
      0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF };
  private static final Random rnd = new Random(12345l);

  public static byte[] get(int size) {
    byte[] array = (byte[]) Array.newInstance(byte.class, size);
    for (int i = 0; i < size; i++)
      array[i] = CACHE[rnd.nextInt(CACHE.length - 1)];
    return array;
  }
}