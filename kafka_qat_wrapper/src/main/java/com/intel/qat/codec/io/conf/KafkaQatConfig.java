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

package com.intel.qat.codec.io.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Kafka QAT configurations loading from kafka-qat.conf present in the classpath
 * with default values. Below are the configurations can be customized,
 *
 * kafka.qat.compression-level= kafka.qat.compression.compression-buffer.size
 * kafka.qat.compression.decompression-buffer.size
 * kafka.qat.compression.buffer-align.size
 * kafka.qat.compression.use-native-buffer
 *
 * kafka.qat.decompression.compression-buffer.size
 * kafka.qat.decompression.compression-buffer.size
 * kafka.qat.decompression.buffer-align.size
 * kafka.qat.decompression.use-native-buffer
 *
 */
public final class KafkaQatConfig {

  private static KafkaQatConfig instance;
  private static final String CONF_FILE_NAME = "kafka-qat.conf";

  private int compressionLevel = DEFAULT_COMPRESSION_LEVEL_VALUE;
  private int compressCompressionBufferSize = DEFAULT_COMPRESS_COMPRESSION_BUFFER_SIZE_VALUE;
  private int compressDecompressionBufferSize = DEFAULT_COMPRESS_DECOMPRESSION_BUFFER_SIZE_VALUE;
  private int compressAlignSize = DEFAULT_COMPRESS_ALIGN_SIZE_VALUE;
  private boolean compressUseNativeBuffer = DEFAULT_COMPRESS_USE_NATIVE_BUFFER_VALUE;
  private int decompressCompressionBufferSize = DEFAULT_DECOMPRESS_COMPRESSION_BUFFER_SIZE_VALUE;
  private int decompressDecompressionBufferSize = DEFAULT_DECOMPRESS_DECOMPRESSION_BUFFER_SIZE_VALUE;
  private int decompressAlignSize = DEFAULT_DECOMPRESS_ALIGN_SIZE_VALUE;
  private boolean decompressUseNativeBuffer = DEFAULT_DECOMPRESS_USE_NATIVE_BUFFER_VALUE;

  private static final String COMPRESSION_LEVEL_KEY = "kafka.qat.compression-level";
  private static final int DEFAULT_COMPRESSION_LEVEL_VALUE = 1;

  private static final String COMPRESS_COMPRESSION_BUFFER_SIZE_KEY = "kafka.qat.compression.compression-buffer.size";
  private static final int DEFAULT_COMPRESS_COMPRESSION_BUFFER_SIZE_VALUE = 32
      * 1024 * 3 / 2;
  private static final String COMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY = "kafka.qat.compression.decompression-buffer.size";
  private static final int DEFAULT_COMPRESS_DECOMPRESSION_BUFFER_SIZE_VALUE = 32
      * 1024;
  private static final String COMPRESS_ALIGN_SIZE_KEY = "kafka.qat.compression.buffer-align.size";
  private static final int DEFAULT_COMPRESS_ALIGN_SIZE_VALUE = 64;

  private static final String COMPRESS_USE_NATIVE_BUFFER_KEY = "kafka.qat.compression.use-native-buffer";
  private static final boolean DEFAULT_COMPRESS_USE_NATIVE_BUFFER_VALUE = true;

  private static final String DECOMPRESS_COMPRESSION_BUFFER_SIZE_KEY = "kafka.qat.decompression.compression-buffer.size";
  private static final int DEFAULT_DECOMPRESS_COMPRESSION_BUFFER_SIZE_VALUE = 32
      * 1024;
  private static final String DECOMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY = "kafka.qat.decompression.decompression-buffer.size";
  private static final int DEFAULT_DECOMPRESS_DECOMPRESSION_BUFFER_SIZE_VALUE = 32
      * 1024 * 3 / 2;
  private static final String DECOMPRESS_ALIGN_SIZE_KEY = "kafka.qat.decompression.buffer-align.size";
  private static final int DEFAULT_DECOMPRESS_ALIGN_SIZE_VALUE = 64;

  private static final String DECOMPRESS_USE_NATIVE_BUFFER_KEY = "kafka.qat.decompression.use-native-buffer";
  private static final boolean DEFAULT_DECOMPRESS_USE_NATIVE_BUFFER_VALUE = true;

  static {
    instance = new KafkaQatConfig();
    try {
      instance.loadConfig();
    } catch (IOException e) {
      System.exit(-1);
    }
  }

  /**
   * Loads the configurations from the kafka-qat.conf.
   *
   * @throws IOException - failure cases
   */
  private void loadConfig() throws IOException {
    if (KafkaQatConfig.class.getClassLoader()
        .getResource(CONF_FILE_NAME) == null) {
      return;
    }
    try (InputStream input = KafkaQatConfig.class.getClassLoader()
        .getResourceAsStream(CONF_FILE_NAME)) {
      Properties prop = new Properties();
      prop.load(input);
      if (prop.get(COMPRESSION_LEVEL_KEY) != null) {
        compressionLevel = Integer
            .parseInt(prop.get(COMPRESSION_LEVEL_KEY).toString().trim());
      }

      if (prop.get(COMPRESS_COMPRESSION_BUFFER_SIZE_KEY) != null) {
        compressCompressionBufferSize = Integer.parseInt(
            prop.get(COMPRESS_COMPRESSION_BUFFER_SIZE_KEY).toString().trim());
      }

      if (prop.get(COMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY) != null) {
        compressDecompressionBufferSize = Integer.parseInt(
            prop.get(COMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY).toString().trim());
      }

      if (prop.get(COMPRESS_ALIGN_SIZE_KEY) != null) {
        compressAlignSize = Integer
            .parseInt(prop.get(COMPRESS_ALIGN_SIZE_KEY).toString().trim());
      }

      if (prop.get(COMPRESS_USE_NATIVE_BUFFER_KEY) != null) {
        compressUseNativeBuffer = Boolean.parseBoolean(
            prop.get(COMPRESS_USE_NATIVE_BUFFER_KEY).toString().trim());
      }

      if (prop.get(DECOMPRESS_COMPRESSION_BUFFER_SIZE_KEY) != null) {
        decompressCompressionBufferSize = Integer.parseInt(
            prop.get(DECOMPRESS_COMPRESSION_BUFFER_SIZE_KEY).toString().trim());
      }

      if (prop.get(DECOMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY) != null) {
        decompressDecompressionBufferSize = Integer.parseInt(prop
            .get(DECOMPRESS_DECOMPRESSION_BUFFER_SIZE_KEY).toString().trim());
      }

      if (prop.get(DECOMPRESS_ALIGN_SIZE_KEY) != null) {
        decompressAlignSize = Integer
            .parseInt(prop.get(DECOMPRESS_ALIGN_SIZE_KEY).toString().trim());
      }

      if (prop.get(DECOMPRESS_USE_NATIVE_BUFFER_KEY) != null) {
        decompressUseNativeBuffer = Boolean.parseBoolean(
            prop.get(DECOMPRESS_USE_NATIVE_BUFFER_KEY).toString().trim());
      }
    }
  }

  public static KafkaQatConfig get() {
    return instance;
  }

  public int getCompressionLevel() {
    return compressionLevel;
  }

  public int getCompressCompressionBufferSize() {
    return compressCompressionBufferSize;
  }

  public int getCompressDecompressionBufferSize() {
    return compressDecompressionBufferSize;
  }

  public int getCompressAlignSize() {
    return compressAlignSize;
  }

  public boolean isCompressUseNativeBuffer() {
    return compressUseNativeBuffer;
  }

  public int getDecompressCompressionBufferSize() {
    return decompressCompressionBufferSize;
  }

  public int getDecompressDecompressionBufferSize() {
    return decompressDecompressionBufferSize;
  }

  public int getDecompressAlignSize() {
    return decompressAlignSize;
  }

  public boolean isDecompressUseNativeBuffer() {
    return decompressUseNativeBuffer;
  }
}
