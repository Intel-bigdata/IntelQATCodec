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

package com.intel.qat.codec.io.buffer;

import java.nio.ByteBuffer;

/**
 * ByteBuffer allocator interface for allocation and release it.
 */
public interface BufferAllocator {

  /**
   * Allocates the ByteBuffer with the parameters specified.
   *
   * @param size - buffer size
   * @param align - align for buffer
   * @param useNative - whether use native or not
   * @param nativeBBUseQzMalloc - whether use qzMalloc
   * @param nativeBBUseNuma - whether use numa
   * @param nativeBBUseForcePinned - whether use forcePinned
   * @return - allocated byte buffer
   */
  ByteBuffer allocate(int size, int align, boolean useNative,
      boolean nativeBBUseQzMalloc, boolean nativeBBUseNuma,
      boolean nativeBBUseForcePinned);

  /**
   * Releases the ByteBuffer.
   *
   * @param buffer - buffer to release
   */
  void release(ByteBuffer buffer);
}
