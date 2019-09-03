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

package com.intel.qat.jni;

import java.nio.ByteBuffer;

import com.intel.qat.util.NativeCodeLoader;

/**
 * JNI bindings to the original C implementation of QatCodec.
 */
public enum QatCodecJNI {
  ;
  static {
    NativeCodeLoader.load();
    init();
  }

  static native void init();
  public static native Object allocNativeBuffer(int capacity, int align);
  public static native long createCompressContext(int level);
  public static native long createDecompressContext();
  public static native void destroyContext(long context);
  public static native int compress(long context, ByteBuffer srcBuffer, int srcOff, int srcLen,
          ByteBuffer destBuffer, int destOff, int maxDestLen);
  public static native int decompress(long context, ByteBuffer srcBuffer, int srcOff, int srcLen,
          ByteBuffer destBuffer, int destOff, int destLen);
  public static native String getLibraryName(int codec);
  public static native Object qzMalloc(long capacity, boolean numa,
      boolean forcePinned);
}

