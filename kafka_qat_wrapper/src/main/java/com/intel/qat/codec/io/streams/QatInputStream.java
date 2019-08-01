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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.intel.qat.codec.io.buffer.BufferAllocator;
import com.intel.qat.codec.io.buffer.CachedNativeByteBufferAllocator;
import com.intel.qat.codec.io.conf.KafkaQatConfig;
import com.intel.qat.codec.io.exception.QatErrorCode;
import com.intel.qat.codec.io.exception.QatIOException;
import com.intel.qat.codec.io.jni.QatNative;
import com.intel.qat.codec.io.util.QatCodec;

/**
 * QatInputStream reads compressed data from the input stream and uncompresses
 * the data as blocks.
 */
public class QatInputStream extends InputStream {
  private InputStream in;

  private byte[] header = new byte[QatCodec.headerSize()];

  private int uncompressedBlockSize;
  private int compressedBlockSize;
  private ByteBuffer uncompressedBuffer;
  private ByteBuffer compressedBuffer;
  private int originalLen;
  private int uncompressedBufferPosition;
  private boolean closed;
  private boolean eof;
  private byte[] tempBuffer;
  private BufferAllocator allocator = CachedNativeByteBufferAllocator.get();

  public QatInputStream(InputStream in) throws IOException {
    this.in = in;
    KafkaQatConfig conf = KafkaQatConfig.get();
    this.uncompressedBlockSize = conf.getDecompressDecompressionBufferSize();
    this.compressedBlockSize = conf.getDecompressCompressionBufferSize();

    this.uncompressedBuffer = allocator.allocate(uncompressedBlockSize,
        conf.getDecompressAlignSize(), conf.isDecompressUseNativeBuffer());
    this.compressedBuffer = allocator.allocate(compressedBlockSize,
        conf.getDecompressAlignSize(), conf.isDecompressUseNativeBuffer());

    uncompressedBufferPosition = originalLen = 0;
    closed = false;
    eof = false;
    tempBuffer = new byte[compressedBlockSize];

    readHeader();
  }

  @Override
  public int available() throws IOException {
    validateStream();
    return originalLen - uncompressedBufferPosition;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    validateStream();
    if (b == null) {
      throw new NullPointerException();
    }
    if (off < 0 || len < 0 || off > b.length - len) {
      throw new ArrayIndexOutOfBoundsException(
          "BlockInputStream read requested lenght " + len + " from offset "
              + off + " in buffer of size " + b.length);
    }

    if (uncompressedBufferPosition == originalLen) {
      decompressData();
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
    validateStream();
    if (uncompressedBufferPosition == originalLen) {
      decompressData();
    }
    if (eof) {
      return -1;
    }
    final int skipped = (int) Math.min(n,
        originalLen - uncompressedBufferPosition);
    uncompressedBufferPosition += skipped;
    uncompressedBuffer.position(uncompressedBufferPosition);
    return skipped;
  }

  private void decompressData() throws IOException {
    int compressedLen = 0;
    try {
      byte[] length = new byte[4];
      if (in.read(length) != 4) {
        eof = true;
        return;
      }
      compressedLen = QatCodec.readInt(length, 0);
    } catch (IOException e) {
      eof = true;
      return;
    }
    if (compressedBuffer.capacity() < compressedLen) {
      throw new IOException(
          "Input Stream is corrupted, compressed length large than "
              + compressedBlockSize);
    }
    readCompressedData(compressedBuffer, compressedLen);
    try {
      originalLen = QatNative.decompress(compressedBuffer, 0, compressedLen,
          uncompressedBuffer, 0, uncompressedBlockSize);
    } catch (Exception e) {
      throw new IOException("Input Stream is corrupted, can't decompress", e);
    }
    uncompressedBuffer.position(0);
    uncompressedBuffer.limit(originalLen);
    uncompressedBufferPosition = 0;
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

  @Override
  public void mark(int readlimit) {
    // unsupported
  }

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
    } finally {
      allocator.release(compressedBuffer);
      allocator.release(uncompressedBuffer);
      tempBuffer = null;
      in = null;
      closed = true;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(in=" + in + ")";
  }

  private void readHeader() throws IOException {
    int readBytes = 0;
    while (readBytes < header.length) {
      int ret = in.read(header, readBytes, header.length - readBytes);
      if (ret == -1) {
        break;
      }
      readBytes += ret;
    }

    if (readBytes == 0) {
      throw new QatIOException(QatErrorCode.EMPTY_INPUT,
          "Cannot decompress empty stream");
    }
    if (readBytes < header.length || !QatCodec.hasMagicHeaderPrefix(header)) {
      // No header found, read fully
      return;
    }
    if (!isValidHeader(header)) {
      throw new QatIOException(QatErrorCode.PARSING_ERROR,
          "Invalid header stream.");
    }
  }

  private boolean isValidHeader(byte[] header) throws IOException {
    QatCodec codec = QatCodec.readHeader(new ByteArrayInputStream(header));
    if (codec.isValidMagicHeader()) {
      if (codec.getVersion() < QatCodec.MINIMUM_COMPATIBLE_VERSION) {
        throw new QatIOException(QatErrorCode.INCOMPATIBLE_VERSION,
            String.format(
                "Compressed with an incompatible codec version %d. At least version %d is required",
                codec.getVersion(), QatCodec.MINIMUM_COMPATIBLE_VERSION));
      }
      return true;
    } else {
      return false;
    }
  }

  private void validateStream() {
    if (closed) {
      throw new IllegalStateException("This output stream is already closed.");
    }
  }
}
