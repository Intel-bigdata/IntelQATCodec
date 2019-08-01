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

package com.intel.qat.codec.io.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.intel.qat.codec.io.buffer.BufferAllocator;
import com.intel.qat.codec.io.buffer.CachedNativeByteBufferAllocator;
import com.intel.qat.codec.io.conf.KafkaQatConfig;
import com.intel.qat.codec.io.jni.QatNative;
import com.intel.qat.codec.io.util.QatCodec;

/**
 * QatOutputStream compresses the data as blocks and writes compressed data into
 * the specified output stream.
 */
public class QatOutputStream extends OutputStream {

  private OutputStream out;
  private boolean closed;

  private int compressionLevel;

  private int uncompressedBlockSize;
  private int compressedBlockSize;

  private ByteBuffer uncompressedBuffer;
  private ByteBuffer compressedBuffer;

  private int uncompressedBufferPosition;
  private byte[] tempBuffer;
  private long context;
  private BufferAllocator allocator = CachedNativeByteBufferAllocator.get();
  private boolean headerWritten;

  public QatOutputStream(OutputStream out) {
    this.out = out;
    KafkaQatConfig conf = KafkaQatConfig.get();

    this.compressionLevel = conf.getCompressionLevel();
    this.uncompressedBlockSize = conf.getCompressDecompressionBufferSize();
    this.compressedBlockSize = conf.getCompressCompressionBufferSize();

    this.uncompressedBuffer = allocator.allocate(uncompressedBlockSize,
        conf.getCompressAlignSize(), conf.isCompressUseNativeBuffer());
    this.compressedBuffer = allocator.allocate(compressedBlockSize,
        conf.getCompressAlignSize(), conf.isCompressUseNativeBuffer());

    uncompressedBufferPosition = 0;
    closed = false;

    tempBuffer = new byte[compressedBlockSize];

    context = QatNative.createCompressContext(compressionLevel);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    validateStream();
    if (b == null) {
      throw new NullPointerException();
    }
    if (off < 0 || len < 0 || off > b.length - len) {
      throw new ArrayIndexOutOfBoundsException(
          "BlockOutputStream write requested lenght " + len + " from offset "
              + off + " in buffer of size " + b.length);
    }

    while (uncompressedBufferPosition + len > uncompressedBlockSize) {
      int left = uncompressedBlockSize - uncompressedBufferPosition;
      uncompressedBuffer.put(b, off, left);
      uncompressedBufferPosition = uncompressedBlockSize;
      compressBufferedData();
      off += left;
      len -= left;
    }
    uncompressedBuffer.put(b, off, len);
    uncompressedBufferPosition += len;
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(int b) throws IOException {
    byte[] oneByte = new byte[1];
    oneByte[0] = (byte) b;
    write(oneByte, 0, 1);
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    try {
      finish();
      out.close();
    } finally {
      closed = true;
      allocator.release(compressedBuffer);
      allocator.release(uncompressedBuffer);
      tempBuffer = null;
      out = null;
      context = 0;
    }
  }

  private void compressBufferedData() throws IOException {
    if (uncompressedBufferPosition == 0) {
      return;
    }
    int compressedLength = QatNative.compress(context, uncompressedBuffer, 0,
        uncompressedBufferPosition, compressedBuffer, 0, compressedBlockSize);
    int header = 0;
    if (!headerWritten) {
      header = writeHeader(tempBuffer);
      headerWritten = true;
    }

    QatCodec.writeInt(tempBuffer, header, compressedLength);
    compressedBuffer.position(0);
    compressedBuffer.limit(compressedLength);
    int totalWritten = 0;
    int off = 4 + header;
    while (totalWritten < compressedLength) {
      int bytesToWrite = Math.min((compressedLength - totalWritten),
          tempBuffer.length - off);
      compressedBuffer.get(tempBuffer, off, bytesToWrite);
      out.write(tempBuffer, 0, bytesToWrite + off);
      totalWritten += bytesToWrite;
      off = 0;
    }
    uncompressedBuffer.clear();
    compressedBuffer.clear();
    uncompressedBufferPosition = 0;
  }

  public void finish() throws IOException {
    validateStream();
    compressBufferedData();
    out.flush();
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    finish();
  }

  private void validateStream() {
    if (context == 0) {
      throw new NullPointerException();
    }
    if (closed) {
      throw new IllegalStateException("This output stream is already closed");
    }
  }

  private int writeHeader(byte[] outputBuffer) {
    return QatCodec.CURRENT_HEADER.writeHeader(outputBuffer, 0);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(out=" + out + ", level="
        + compressionLevel + ", blockSize=" + uncompressedBlockSize + ")";
  }
}
