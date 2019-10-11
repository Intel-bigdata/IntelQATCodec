/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.qat.util.buffer;

import com.intel.qat.jni.QatCodecJNI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Cached buffer
 */
public class CachedBufferAllocator implements BufferAllocator 
{
    private static final Logger LOG = LoggerFactory.getLogger(CachedBufferAllocator.class);

    private static BufferAllocatorFactory factory = new BufferAllocatorFactory()
    {
        @Override
        public BufferAllocator getBufferAllocator(int bufferSize)
        {
            return CachedBufferAllocator.getAllocator(bufferSize);
        }
    };

    public static void setBufferAllocatorFactory(BufferAllocatorFactory factory)
    {
        assert (factory != null);
        CachedBufferAllocator.factory = factory;
    }

    public static BufferAllocatorFactory getBufferAllocatorFactory()
    {
        return factory;
    }

    /**
     * Use SoftReference so that having this queueTable does not prevent the GC of CachedBufferAllocator instances
     */
    private static final Map<Integer, SoftReference<CachedBufferAllocator>> queueTable = new HashMap<Integer, SoftReference<CachedBufferAllocator>>();

    private final int bufferSize;
    private final Deque<ByteBuffer> directByteBufferQueue;
	private final Deque<byte[]> byteArrayQueue;

    public CachedBufferAllocator(int bufferSize)
    {
        this.bufferSize = bufferSize;
		this.byteArrayQueue = new ArrayDeque<byte[]>();
        this.directByteBufferQueue = new ArrayDeque<ByteBuffer>();
    }

    public static synchronized CachedBufferAllocator getAllocator(int bufferSize)
    {
        CachedBufferAllocator result = null;

        if (queueTable.containsKey(bufferSize)) {
            result = queueTable.get(bufferSize).get();
        }
        if (result == null) {
            result = new CachedBufferAllocator(bufferSize);
            queueTable.put(bufferSize, new SoftReference<CachedBufferAllocator>(result));
        }
        return result;
    }

    @Override
    public ByteBuffer allocateDirectByteBuffer(boolean useNativeBuffer, int size, int align)
    {
        synchronized (this) {
            if (directByteBufferQueue.isEmpty()) {
                if (useNativeBuffer) {
                    try {
                        return (ByteBuffer) QatCodecJNI.allocNativeBuffer(size, align);
                    } catch (Throwable t) {
                        LOG.error("Native buffer allocation is failed and fall back to direct allocation.");
                        return ByteBuffer.allocateDirect(size);
                    }
                }
                return ByteBuffer.allocateDirect(size);
            }
            else {
                return directByteBufferQueue.pollFirst();
            }
        }
    }

    @Override
    public void releaseDirectByteBuffer(ByteBuffer buffer)
    {
        synchronized (this) {
            directByteBufferQueue.addLast(buffer);
        }
    }

   @Override
    public byte[] allocateByteArray(int size)
    {
        synchronized (this) {
            if (byteArrayQueue.isEmpty()) {
                return new byte[size];
            }
            else {
                return byteArrayQueue.pollFirst();
            }
        }
    }

    @Override
    public void releaseByteArray(byte[] buffer)
    {
        synchronized (this) {
            byteArrayQueue.addLast(buffer);
        }
    }
}
