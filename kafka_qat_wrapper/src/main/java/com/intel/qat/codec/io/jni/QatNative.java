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

package com.intel.qat.codec.io.jni;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intel.qat.codec.io.exception.QatIOException;
import com.intel.qat.codec.io.util.QatLoader;

/**
 * JNI bindings to the Kafka qat codec C implementation.
 */
public final class QatNative {
  private static final Logger LOG = LoggerFactory.getLogger(QatNative.class);

  static {
    try {
      QatLoader.loadQatApi();
    } catch (QatIOException e) {
      throw new ExceptionInInitializerError(e);
    }
    init();
    LOG.info("Loaded qat library with name " + getLibraryName() + ".");
  }

  private QatNative() {
  }

  /**
   * Initializes the qat compression library.
   */
  public static native void init();

  /**
   * Gets the qat library name.
   *
   * @return library name
   */
  public static native String getLibraryName();

  /**
   * Allocates the native byte buffer.
   *
   * @param capacity
   *          - Capacity of the ByteBuffer
   * @param align
   *          - Align of the ByteBuffer
   * @return - natively allocated ByteBuffer
   */
  public static native Object allocNativeBuffer(int capacity, int align);

  /**
   * Allocates the native byte buffer using qzip qzMalloc.
   *
   * @param capacity
   *          - Capacity of the ByteBuffer
   * @param numa
   *          - Allocate memory from NUMA node for qzMalloc
   * @param forcePinned
   *          - Allocate continuous memory for qzMalloc
   * @return - ByteBuffer created using qzMalloc
   */
  public static native Object qzMalloc(long capacity, boolean numa,
      boolean forcePinned);

  /**
   * Creates the compress context.
   *
   * @param level
   *          - Compression level
   * @return - compress context.
   */
  public static native long createCompressContext(int level);

  /**
   * Compresses the data from the srcBuffer to the destBuffer.
   *
   * @param context
   * @param srcBuffer
   * @param srcOff
   * @param srcLen
   * @param destBuffer
   * @param destOff
   * @param maxDestLen
   * @return - Compressed data length.
   */
  public static native int compress(long context, ByteBuffer srcBuffer,
      int srcOff, int srcLen, ByteBuffer destBuffer, int destOff,
      int maxDestLen);

  /**
   * Uncompresses the data from the srcBuffer to the destBuffer.
   *
   * @param srcBuffer
   * @param srcOff
   * @param srcLen
   * @param destBuffer
   * @param destOff
   * @param destLen
   * @return Uncompressed data length.
   */
  public static native int decompress(ByteBuffer srcBuffer, int srcOff,
      int srcLen, ByteBuffer destBuffer, int destOff, int destLen);
}
