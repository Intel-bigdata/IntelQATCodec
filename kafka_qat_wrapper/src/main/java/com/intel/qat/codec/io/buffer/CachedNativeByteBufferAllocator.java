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
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intel.qat.codec.io.jni.QatNative;

/**
 * Cached implementation of ByteBuffer, which tries to create from native
 * allocation if specified and falls back direct byte buffer for any failures.
 */
public final class CachedNativeByteBufferAllocator implements BufferAllocator {
  private static final Logger LOG = LoggerFactory
      .getLogger(CachedNativeByteBufferAllocator.class);
  private static BufferAllocator instance;
  private final Deque<ByteBuffer> bufferQueue;

  public static BufferAllocator get() {
    if (instance == null) {
      synchronized (CachedNativeByteBufferAllocator.class) {
        if (instance == null) {
          instance = new CachedNativeByteBufferAllocator();
        }
      }
    }
    return instance;
  }

  private CachedNativeByteBufferAllocator() {
    this.bufferQueue = new ArrayDeque<ByteBuffer>();
  }

  @Override
  public ByteBuffer allocate(int size, int align, boolean useNative,
      boolean nativeBBUseQzMalloc, boolean nativeBBUseNuma,
      boolean nativeBBUseForcePinned) {
    synchronized (this) {
      if (bufferQueue.isEmpty()) {
        return getInstance(size, align, useNative, nativeBBUseQzMalloc,
            nativeBBUseNuma, nativeBBUseForcePinned);
      } else {
        return bufferQueue.pollFirst();
      }
    }
  }

  /**
   * Creates ByteBuffer instance from native or direct byte buffer based on the
   * configuration.
   *
   * @param size
   * @param align
   * @param useNative
   * @param nativeBBUseForcePinned
   * @param nativeBBUseNuma
   * @param nativeBBUseQzMalloc
   * @return
   */
  private ByteBuffer getInstance(int size, int align, boolean useNative,
      boolean nativeBBUseQzMalloc, boolean nativeBBUseNuma,
      boolean nativeBBUseForcePinned) {
    if (useNative) {
      try {
        if (nativeBBUseQzMalloc) {
          try {
            return (ByteBuffer) QatNative.qzMalloc(size, nativeBBUseNuma,
                nativeBBUseForcePinned);
          } catch (Throwable e) {
            LOG.warn("Failed to create native byte buffer using qzMalloc,"
                + " falling back to creating native byte buffer without qzMalloc.", e);
          }
        }
        return (ByteBuffer) QatNative.allocNativeBuffer(size, align);
      } catch (Throwable e) {
        LOG.warn("Failed to create native byte buffer, "
            + "falling back to creating direct byte buffer.", e);
        return ByteBuffer.allocateDirect(size);
      }
    } else {
      return ByteBuffer.allocateDirect(size);
    }
  }

  @Override
  public void release(ByteBuffer buffer) {
    buffer.clear();
    synchronized (this) {
      bufferQueue.addLast(buffer);
    }
  }
}
