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

import com.intel.qat.codec.io.buffer.BufferAllocator;
import com.intel.qat.codec.io.buffer.BufferAllocatorFactory;
import com.intel.qat.codec.io.buffer.CachedBufferAllocator;
import com.intel.qat.codec.io.util.Qat;
import com.intel.qat.codec.io.util.QatCodec;

public class QatOutputStream extends OutputStream {

  private static final int MIN_BLOCK_SIZE = 1 * 1024; // 1kb as min block size
  private static final int DEFAULT_BLOCK_SIZE = 32 * 1024; // 32kb as default

  private final OutputStream out;
  private final int blockSize;

  private final BufferAllocator inputBufferAllocator;
  private final BufferAllocator outputBufferAllocator;

  private byte[] inputBuffer;
  private byte[] outputBuffer;

  private int inputCursor = 0;
  private int outputCursor = 0;

  private boolean headerWritten;
  private boolean closed;

  public QatOutputStream(OutputStream out) {
    this(out, DEFAULT_BLOCK_SIZE);
  }

  public QatOutputStream(OutputStream out, int blockSize) {
    this(out, blockSize, CachedBufferAllocator.getBufferAllocatorFactory());
  }

  public QatOutputStream(OutputStream out, int blockSize,
      BufferAllocatorFactory bufferAllocatorFactory) {
    this.out = out;
    this.blockSize = Math.max(MIN_BLOCK_SIZE, blockSize);
    int outputSize = QatCodec.HEADER_SIZE + 4
        + Qat.maxCompressedLength(blockSize);
    this.inputBufferAllocator = bufferAllocatorFactory
        .getBufferAllocator(blockSize);
    this.outputBufferAllocator = bufferAllocatorFactory
        .getBufferAllocator(outputSize);

    inputBuffer = inputBufferAllocator.allocate(blockSize);
    outputBuffer = outputBufferAllocator.allocate(outputSize);
  }

  @Override
  public void write(byte[] b, int byteOffset, int byteLength)
      throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }
    int cursor = 0;
    while (cursor < byteLength) {
      int readLen = Math.min(byteLength - cursor, blockSize - inputCursor);
      if (readLen > 0) {
        System.arraycopy(b, byteOffset + cursor, inputBuffer, inputCursor,
            readLen);
        inputCursor += readLen;
      }
      if (inputCursor < blockSize) {
        return;
      }
      compressInput();
      cursor += readLen;
    }
  }

  @Override
  public void write(int b) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }
    if (inputCursor >= inputBuffer.length) {
      compressInput();
    }
    inputBuffer[inputCursor++] = (byte) b;
  }

  @Override
  public void flush() throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }
    compressInput();
    dumpOutput();
    out.flush();
  }

  private boolean hasSufficientOutputBufferFor(int inputSize) {
    int maxCompressedSize = Qat.maxCompressedLength(inputSize);
    return maxCompressedSize < outputBuffer.length - outputCursor - 4;
  }

  private void dumpOutput() throws IOException {
    if (outputCursor > 0) {
      out.write(outputBuffer, 0, outputCursor);
      outputCursor = 0;
    }
  }

  private void compressInput() throws IOException {
    if (!headerWritten) {
      outputCursor = writeHeader();
      headerWritten = true;
    }
    if (inputCursor <= 0) {
      return;
    }
    if (!hasSufficientOutputBufferFor(inputCursor)) {
      dumpOutput();
    }
    byte[] tempOp = new byte[inputBuffer.length];
    int compressedSize = Qat.compress(inputBuffer, 0, inputCursor, tempOp, 0);
    System.arraycopy(tempOp, 0, outputBuffer, outputCursor + 4, compressedSize);

    QatCodec.writeInt(outputBuffer, outputCursor, compressedSize);
    outputCursor += 4 + compressedSize;
    inputCursor = 0;
  }

  private int writeHeader() {
    return QatCodec.currentHeader.writeHeader(outputBuffer, 0);
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    try {
      flush();
      out.close();
    } finally {
      closed = true;
      inputBufferAllocator.release(inputBuffer);
      outputBufferAllocator.release(outputBuffer);
      inputBuffer = null;
      outputBuffer = null;
    }
  }
}
