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

package com.intel.qat.codec.io.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Kafka QAT header while writing or reading to/from the streams.
 */
public class QatCodec {

  private static final byte[] MAGIC_HEADER = new byte[] { -126, 'Q', 'A', 'T',
      'C', 'O', 'M', 0 };
  private static final int MAGIC_LEN = MAGIC_HEADER.length;
  private static final int HEADER_SIZE = MAGIC_LEN + 8;
  private static final int MAGIC_HEADER_HEAD = readInt(MAGIC_HEADER, 0);

  static {
    assert (MAGIC_HEADER_HEAD < 0);
  }

  private static final int DEFAULT_VERSION = 1;
  public static final int MINIMUM_COMPATIBLE_VERSION = 1;
  public static final QatCodec CURRENT_HEADER = new QatCodec(MAGIC_HEADER,
      DEFAULT_VERSION, MINIMUM_COMPATIBLE_VERSION);

  private final byte[] magic;
  private final int version;

  private final int compatibleVersion;
  private final byte[] headerArray;

  public QatCodec(byte[] magic, int version, int compatibleVersion) {
    this.magic = magic;
    this.version = version;
    this.compatibleVersion = compatibleVersion;

    ByteArrayOutputStream header = new ByteArrayOutputStream(HEADER_SIZE);
    DataOutputStream d = new DataOutputStream(header);
    try {
      d.write(magic, 0, MAGIC_LEN);
      d.writeInt(version);
      d.writeInt(compatibleVersion);
      d.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    headerArray = header.toByteArray();
  }

  public static byte[] getMagicHeader() {
    return MAGIC_HEADER.clone();
  }

  @Override
  public String toString() {
    return String.format("version:%d, compatible version:%d", version,
        compatibleVersion);
  }

  public static int headerSize() {
    return HEADER_SIZE;
  }

  public int writeHeader(byte[] dst, int dstOffset) {
    System.arraycopy(headerArray, 0, dst, dstOffset, headerArray.length);
    return headerArray.length;
  }

  public boolean isValidMagicHeader() {
    return Arrays.equals(MAGIC_HEADER, magic);
  }

  public int getVersion() {
    return version;
  }

  public static boolean hasMagicHeaderPrefix(byte[] b) {
    int limit = Math.min(MAGIC_LEN, b.length);
    int i = 0;
    while (i < limit) {
      if (b[i] != MAGIC_HEADER[i]) {
        return false;
      }
      ++i;
    }
    return true;
  }

  public static QatCodec readHeader(InputStream in) throws IOException {
    DataInputStream d = new DataInputStream(in);
    byte[] magic = new byte[MAGIC_LEN];
    d.readFully(magic, 0, MAGIC_LEN);
    int version = d.readInt();
    int compatibleVersion = d.readInt();
    return new QatCodec(magic, version, compatibleVersion);
  }

  public static void writeInt(byte[] dst, int offset, int v) {
    dst[offset] = (byte) ((v >> 24) & 0xFF);
    dst[offset + 1] = (byte) ((v >> 16) & 0xFF);
    dst[offset + 2] = (byte) ((v >> 8) & 0xFF);
    dst[offset + 3] = (byte) ((v >> 0) & 0xFF);
  }

  public static int readInt(byte[] buffer, int pos) {
    int b1 = (buffer[pos] & 0xFF) << 24;
    int b2 = (buffer[pos + 1] & 0xFF) << 16;
    int b3 = (buffer[pos + 2] & 0xFF) << 8;
    int b4 = buffer[pos + 3] & 0xFF;
    return b1 | b2 | b3 | b4;
  }
}
