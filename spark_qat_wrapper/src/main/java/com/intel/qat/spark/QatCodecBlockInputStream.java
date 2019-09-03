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

package com.intel.qat.spark;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.nio.ByteBuffer;

import com.intel.qat.util.buffer.*;
import com.intel.qat.jni.QatCodecJNI;
import com.sun.tools.javac.util.Assert;

/**
 * {@link InputStream} implementation to decompress data written with
 * {@link QatCodecBlockOutputStream}. This class is not
 * thread-safe and does not support {@link #mark(int)}/{@link #reset()}.
 * @see QatCodecBlockOutputStream
 */
public final class QatCodecBlockInputStream extends FilterInputStream {
  private long context;
  private int compressedBlockSize;
  private int uncompressedBlockSize;
  private final BufferAllocator compressedBufferAllocator;
  private final BufferAllocator uncompressedBufferAllocator;
  private final ByteBuffer compressedBuffer;
  private final ByteBuffer uncompressedBuffer;
  private int originalLen;
  private int uncompressedBufferPosition;
  private boolean closed;
  private boolean eof;
  private byte[] tempBuffer;
  private final BufferAllocator tempBufferAllocator;

  /**
   * Create a new {@link InputStream}.
   *
   * @param in                the {@link InputStream} to poll
   * @param blockSize         the maximum number of bytes to try to compress at once,
   *                          must be >= 32k
   */
  public QatCodecBlockInputStream(InputStream in, int blockSize, boolean useNativeBuffer) {
    this(in, blockSize, useNativeBuffer, true, true, false);
  }

  public QatCodecBlockInputStream(InputStream in, int blockSize,
      boolean useNativeBuffer, boolean useQzMalloc, boolean useForcePinned, boolean useNuma) {
    super(in);
    this.uncompressedBlockSize = blockSize;
    this.compressedBlockSize = blockSize * 3 / 2;
    this.uncompressedBufferAllocator = CachedBufferAllocator
        .getBufferAllocatorFactory().getBufferAllocator(uncompressedBlockSize);
    this.compressedBufferAllocator = CachedBufferAllocator
        .getBufferAllocatorFactory().getBufferAllocator(compressedBlockSize);
    this.uncompressedBuffer = uncompressedBufferAllocator
        .allocateDirectByteBuffer(useNativeBuffer, uncompressedBlockSize, 64,
            useQzMalloc, useForcePinned, useNuma);
    this.compressedBuffer = compressedBufferAllocator.allocateDirectByteBuffer(
        useNativeBuffer, compressedBlockSize, 64, useQzMalloc, useForcePinned,
        useNuma);

    if(null!=uncompressedBuffer){
      uncompressedBuffer.clear();
    }
    if(null!=compressedBuffer){
      compressedBuffer.clear();
    }

    uncompressedBufferPosition = originalLen = 0;
    closed = false;
    eof = false;
    tempBufferAllocator = CachedBufferAllocator
        .getBufferAllocatorFactory().getBufferAllocator(compressedBlockSize);
    tempBuffer = tempBufferAllocator
        .allocateByteArray(compressedBlockSize);

    context = QatCodecJNI.createDecompressContext();
  }

  private void checkStream() {
    if (context == 0) {
      throw new NullPointerException("This output stream's context is not initialized");
    }
    if (closed) {
      throw new IllegalStateException("This output stream is already closed");
    }
  }

  @Override
  public int available() throws IOException {
    checkStream();
    return originalLen - uncompressedBufferPosition;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    checkStream();
    if (b == null) {
      throw new NullPointerException();
    }
    if (off < 0 || len < 0 || off > b.length - len) {
      throw new ArrayIndexOutOfBoundsException("BlockInputStream read requested lenght " + len
          + " from offset " + off + " in buffer of size " + b.length);
    }

    if (uncompressedBufferPosition == originalLen) {
      refill();
    }
    if (eof) {
      return -1;
    }
    len = Math.min(len, originalLen - uncompressedBufferPosition);
    uncompressedBuffer.get(b, off, len);
    uncompressedBufferPosition += len;
    return len;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read() throws IOException {
    byte[] oneByte = new byte[1];
    int result = read(oneByte, 0, 1);
    if (result > 0) {
      return oneByte[0] & 0xff;
    } else {
      return result;
    }
  }

  @Override
  public long skip(long n) throws IOException {
    checkStream();
    if (uncompressedBufferPosition == originalLen) {
      refill();
    }
    if (eof) {
      return -1;
    }
    final int skipped = (int) Math.min(n, originalLen - uncompressedBufferPosition);
    uncompressedBufferPosition += skipped;
    uncompressedBuffer.position(uncompressedBufferPosition);
    return skipped;
  }

  private void refill() throws IOException {
    int compressedLen = 0;
    try {
       compressedLen = readCompressedBlockLength();
    } catch (IOException e) {
        eof = true;
        return;
    }
    if (compressedBuffer.capacity() < compressedLen) {
      throw new IOException("Input Stream is corrupted, compressed length large than " + compressedBlockSize);
    }
    readCompressedData(compressedBuffer, compressedLen);
    try {
      final int uncompressed_size = QatCodecJNI.decompress(context,
          compressedBuffer, 0, compressedLen,
          uncompressedBuffer, 0, uncompressedBlockSize);
      originalLen = uncompressed_size;
    } catch (QatCodecException e) {
      throw new IOException("Input Stream is corrupted, can't decompress", e);
    }
    uncompressedBuffer.position(0);
    uncompressedBuffer.limit(originalLen);
    uncompressedBufferPosition = 0;
  }

  private int readCompressedBlockLength() throws IOException {
    int b1 = in.read();
    int b2 = in.read();
    int b3 = in.read();
    int b4 = in.read();
    if ((b1 | b2 | b3 | b4) < 0)
      throw new EOFException();
    return ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
  }

  private void readCompressedData(ByteBuffer b, int len) throws IOException {
    int read = 0;
    assert b.capacity() >= len;
    b.clear();
    while (read < len) {
      final int bytesToRead = Math.min(len - read, tempBuffer.length);
      final int r = in.read(tempBuffer, 0, bytesToRead);
      if (r < 0) {
        throw new EOFException("Unexpected end of block in input stream");
      }
      read += r;
      b.put(tempBuffer, 0, r);
    }
    b.flip();
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @SuppressWarnings("sync-override")
  @Override
  public void mark(int readlimit) {
    // unsupported
  }

  @SuppressWarnings("sync-override")
  @Override
  public void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    try {
      in.close();
    }
    finally {
      uncompressedBufferAllocator.releaseDirectByteBuffer(uncompressedBuffer);
      compressedBufferAllocator.releaseDirectByteBuffer(compressedBuffer);
      tempBufferAllocator.releaseByteArray(tempBuffer);
      tempBuffer = null;
      in = null;
      QatCodecJNI.destroyContext(context);
      context = 0;
      closed = true;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(in=" + in
        + ")";
  }
}
