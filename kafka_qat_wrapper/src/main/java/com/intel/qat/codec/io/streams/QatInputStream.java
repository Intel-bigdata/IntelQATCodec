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
import java.io.IOException;
import java.io.InputStream;

import com.intel.qat.codec.io.exception.QatErrorCode;
import com.intel.qat.codec.io.exception.QatIOException;
import com.intel.qat.codec.io.util.Qat;
import com.intel.qat.codec.io.util.QatCodec;

public class QatInputStream extends InputStream {
  private boolean finishedReading = false;
  private final InputStream in;

  private byte[] compressed;
  private byte[] uncompressed;

  private int uncompressedCursor = 0;
  private int uncompressedLimit = 0;

  private byte[] header = new byte[QatCodec.headerSize()];

  public QatInputStream(InputStream input) throws IOException {
    this.in = input;
    readHeader();
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
      readFully(header, readBytes);
      return;
    }
  }

  private boolean isValidHeader(byte[] header) throws IOException {
    QatCodec codec = QatCodec.readHeader(new ByteArrayInputStream(header));
    if (codec.isValidMagicHeader()) {
      if (codec.version < QatCodec.MINIMUM_COMPATIBLE_VERSION) {
        throw new QatIOException(QatErrorCode.INCOMPATIBLE_VERSION,
            String.format(
                "Compressed with an incompatible codec version %d. At least version %d is required",
                codec.version, QatCodec.MINIMUM_COMPATIBLE_VERSION));
      }
      return true;
    } else {
      return false;
    }
  }

  private void readFully(byte[] fragment, int fragmentLength)
      throws IOException {
    if (fragmentLength == 0) {
      finishedReading = true;
      return;
    }
    int compressedLen = Math.max(8 * 1024, fragmentLength);
    compressed = new byte[compressedLen]; // 8K
    Qat.arraycopy(fragment, 0, compressed, 0, fragmentLength);
    int cursor = fragmentLength;
    for (int readBytes = 0; (readBytes = in.read(compressed, cursor,
        compressed.length - cursor)) != -1;) {
      cursor += readBytes;
      if (cursor >= compressed.length) {
        byte[] newBuf = new byte[(compressed.length * 2)];
        Qat.arraycopy(compressed, 0, newBuf, 0, compressed.length);
        compressed = newBuf;
      }
    }

    finishedReading = true;

    // Uncompress
    // int uncompressedLength = Qat.uncompressedLength(compressed, 0, cursor);
    int uncompressedLength = compressedLen * 2;
    uncompressed = new byte[uncompressedLength];
    int uncompress = Qat.uncompress(compressed, 0, cursor, uncompressed, 0);
    this.uncompressedCursor = 0;
    this.uncompressedLimit = uncompress;
    // this.uncompressedLimit = uncompressedLength;
  }

  @Override
  public int read(byte[] b, int byteOffset, int byteLength) throws IOException {
    int writtenBytes = 0;
    while (writtenBytes < byteLength) {
      if (uncompressedCursor >= uncompressedLimit) {
        if (hasNextChunk()) {
          continue;
        } else {
          return writtenBytes == 0 ? -1 : writtenBytes;
        }
      }
      int bytesToWrite = Math.min(uncompressedLimit - uncompressedCursor,
          byteLength - writtenBytes);
      Qat.arraycopy(uncompressed, uncompressedCursor, b,
          byteOffset + writtenBytes, bytesToWrite);
      writtenBytes += bytesToWrite;
      uncompressedCursor += bytesToWrite;
    }
    return writtenBytes;
  }

  private int readNext(byte[] dest, int offset, int len) throws IOException {
    int readBytes = 0;
    while (readBytes < len) {
      int ret = in.read(dest, readBytes + offset, len - readBytes);
      if (ret == -1) {
        finishedReading = true;
        return readBytes;
      }
      readBytes += ret;
    }
    return readBytes;
  }

  private boolean hasNextChunk() throws IOException {
    if (finishedReading) {
      return false;
    }

    uncompressedCursor = 0;
    uncompressedLimit = 0;

    int readBytes = readNext(header, 0, 4);
    if (readBytes < 4) {
      return false;
    }

    int chunkSize = QatCodec.readInt(header, 0);
    if (chunkSize == QatCodec.MAGIC_HEADER_HEAD) {
      int remainingHeaderSize = QatCodec.headerSize() - 4;
      readBytes = readNext(header, 4, remainingHeaderSize);
      if (readBytes < remainingHeaderSize) {
        throw new QatIOException(QatErrorCode.FAILED_TO_UNCOMPRESS,
            String.format("Insufficient header size in a concatenated block"));
      }

      if (isValidHeader(header)) {
        return hasNextChunk();
      } else {
        return false;
      }
    }

    // extend the compressed data buffer size
    if (compressed == null || chunkSize > compressed.length) {
      compressed = new byte[chunkSize];
    }
    readBytes = 0;
    while (readBytes < chunkSize) {
      int ret = in.read(compressed, readBytes, chunkSize - readBytes);
      if (ret == -1) {
        break;
      }
      readBytes += ret;
    }
    if (readBytes < chunkSize) {
      throw new IOException("failed to read chunk");
    }
    int uncompressedLength = compressed.length * 2;
    // if (uncompressed == null || uncompressedLength > uncompressed.length) {
    uncompressed = new byte[uncompressedLength];
    // }
    int actualUncompressedLength = Qat.uncompress(compressed, 0, chunkSize,
        uncompressed, 0);
    // if (uncompressedLength != actualUncompressedLength) {
    // throw new QatIOException(QatErrorCode.INVALID_CHUNK_SIZE, String.format(
    // "expected %,d bytes, but decompressed chunk has %,d bytes",
    // uncompressedLength, actualUncompressedLength));
    // }
    uncompressedLimit = actualUncompressedLength;

    return true;
  }

  @Override
  public int read() throws IOException {
    if (uncompressedCursor < uncompressedLimit) {
      return uncompressed[uncompressedCursor++] & 0xFF;
    } else {
      if (hasNextChunk()) {
        return read();
      } else {
        return -1;
      }
    }
  }

  @Override
  public int available() throws IOException {
    if (uncompressedCursor < uncompressedLimit) {
      return uncompressedLimit - uncompressedCursor;
    } else {
      if (hasNextChunk()) {
        return uncompressedLimit - uncompressedCursor;
      } else {
        return 0;
      }
    }
  }

  @Override
  public void close() throws IOException {
    compressed = null;
    uncompressed = null;
    if (in != null) {
      in.close();
    }
  }
}
