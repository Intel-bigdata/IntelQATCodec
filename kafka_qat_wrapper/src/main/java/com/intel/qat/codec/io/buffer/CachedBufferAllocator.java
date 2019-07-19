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

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CachedBufferAllocator implements BufferAllocator {
  private static BufferAllocatorFactory factory = new BufferAllocatorFactory() {
    @Override
    public BufferAllocator getBufferAllocator(int bufferSize) {
      return CachedBufferAllocator.getAllocator(bufferSize);
    }
  };

  public static void setBufferAllocatorFactory(BufferAllocatorFactory factory) {
    assert (factory != null);
    CachedBufferAllocator.factory = factory;
  }

  public static BufferAllocatorFactory getBufferAllocatorFactory() {
    return factory;
  }

  private static final Map<Integer, SoftReference<CachedBufferAllocator>> queueTable = new HashMap<Integer, SoftReference<CachedBufferAllocator>>();

  private final Deque<byte[]> bufferQueue;

  public CachedBufferAllocator(int bufferSize) {
    this.bufferQueue = new ArrayDeque<byte[]>();
  }

  public static synchronized CachedBufferAllocator getAllocator(
      int bufferSize) {
    CachedBufferAllocator result = null;

    if (queueTable.containsKey(bufferSize)) {
      result = queueTable.get(bufferSize).get();
    }
    if (result == null) {
      result = new CachedBufferAllocator(bufferSize);
      queueTable.put(bufferSize,
          new SoftReference<CachedBufferAllocator>(result));
    }
    return result;
  }

  @Override
  public byte[] allocate(int size) {
    synchronized (this) {
      if (bufferQueue.isEmpty()) {
        return new byte[size];
      } else {
        return bufferQueue.pollFirst();
      }
    }
  }

  @Override
  public void release(byte[] buffer) {
    synchronized (this) {
      bufferQueue.addLast(buffer);
    }
  }
}
