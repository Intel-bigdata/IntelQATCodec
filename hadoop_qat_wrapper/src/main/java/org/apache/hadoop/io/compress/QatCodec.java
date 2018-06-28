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

package org.apache.hadoop.io.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.conf.QatConfigurationKeys;
import org.apache.hadoop.io.compress.qat.QatCompressor;
import org.apache.hadoop.io.compress.qat.QatDecompressor;
import org.apache.hadoop.io.compress.qat.QatDecompressor.QatDirectDecompressor;
import org.apache.hadoop.util.QatNativeCodeLoader;

/**
 * This class creates qat compressors/decompressors.
 */
public class QatCodec implements Configurable, CompressionCodec, DirectDecompressionCodec {
  Configuration conf;

  /**
   * Set the configuration to be used by this object.
   *
   * @param conf the configuration object.
   */
  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Return the configuration used by this object.
   *
   * @return the configuration object used by this objec.
   */
  @Override
  public Configuration getConf() {
    return conf;
  }

  /**
   * Are the native qat libraries loaded & initialized?
   */
  public static void checkNativeCodeLoaded() {
      if (!QatNativeCodeLoader.buildSupportsQat()) {
        throw new RuntimeException("native qat library not available: " +
            "this version of libhadoop was built without " +
            "qat support.");
      }
      if (!QatCompressor.isNativeCodeLoaded()) {
        throw new RuntimeException("native qat library not available: " +
            "QatCompressor has not been loaded AMAC.");
      }
      if (!QatDecompressor.isNativeCodeLoaded()) {
        throw new RuntimeException("native qat library not available: " +
            "QatDecompressor has not been loaded.");
      }
  }

  public static boolean isNativeCodeLoaded() {
    return QatCompressor.isNativeCodeLoaded() &&
        QatDecompressor.isNativeCodeLoaded();
  }

  public static String getLibraryName() {
    return QatCompressor.getLibraryName();
  }

  /**
   * Create a {@link CompressionOutputStream} that will write to the given
   * {@link OutputStream}.
   *
   * @param out the location for the final output stream
   * @return a stream the user can write uncompressed data to have it compressed
   * @throws IOException
   */
  @Override
  public CompressionOutputStream createOutputStream(OutputStream out)
      throws IOException {
    return createOutputStream(out, createCompressor());
  }

  /**
   * Create a {@link CompressionOutputStream} that will write to the given
   * {@link OutputStream} with the given {@link Compressor}.
   *
   * @param out        the location for the final output stream
   * @param compressor compressor to use
   * @return a stream the user can write uncompressed data to have it compressed
   * @throws IOException
   */
  @Override
  public CompressionOutputStream createOutputStream(OutputStream out,
                                                    Compressor compressor)
      throws IOException {
    checkNativeCodeLoaded();
    int bufferSize = conf.getInt(
    		QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY,
    		QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_DEFAULT);

    int compressionOverhead = (bufferSize / 6) + 32;

    return new BlockCompressorStream(out, compressor, bufferSize,
        compressionOverhead);
  }

  /**
   * Get the type of {@link Compressor} needed by this {@link CompressionCodec}.
   *
   * @return the type of compressor needed by this codec.
   */
  @Override
  public Class<? extends Compressor> getCompressorType() {
    checkNativeCodeLoaded();
    return QatCompressor.class;
  }

  /**
   * Create a new {@link Compressor} for use by this {@link CompressionCodec}.
   *
   * @return a new compressor for use by this codec
   */
  @Override
  public Compressor createCompressor() {
    checkNativeCodeLoaded();
    int bufferSize = conf.getInt(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_DEFAULT);
    boolean useNativeBB = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_DEFAULT);
    boolean forcePinned = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_DEFAULT);
    boolean numa = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_DEFAULT);
    return new QatCompressor(bufferSize, useNativeBB, forcePinned, numa);
  }

  /**
   * Create a {@link CompressionInputStream} that will read from the given
   * input stream.
   *
   * @param in the stream to read compressed bytes from
   * @return a stream to read uncompressed bytes from
   * @throws IOException
   */
  @Override
  public CompressionInputStream createInputStream(InputStream in)
      throws IOException {
    return createInputStream(in, createDecompressor());
  }

  /**
   * Create a {@link CompressionInputStream} that will read from the given
   * {@link InputStream} with the given {@link Decompressor}.
   *
   * @param in           the stream to read compressed bytes from
   * @param decompressor decompressor to use
   * @return a stream to read uncompressed bytes from
   * @throws IOException
   */
  @Override
  public CompressionInputStream createInputStream(InputStream in,
                                                  Decompressor decompressor)
      throws IOException {
    checkNativeCodeLoaded();
    return new BlockDecompressorStream(in, decompressor, conf.getInt(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_DEFAULT));
  }

  /**
   * Get the type of {@link Decompressor} needed by this {@link CompressionCodec}.
   *
   * @return the type of decompressor needed by this codec.
   */
  @Override
  public Class<? extends Decompressor> getDecompressorType() {
    checkNativeCodeLoaded();
    return QatDecompressor.class;
  }

  /**
   * Create a new {@link Decompressor} for use by this {@link CompressionCodec}.
   *
   * @return a new decompressor for use by this codec
   */
  @Override
  public Decompressor createDecompressor() {
    checkNativeCodeLoaded();
    int bufferSize = conf.getInt(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_DEFAULT);
    boolean useNativeBB = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_DEFAULT);
    boolean forcePinned = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_DEFAULT);
    boolean numa = conf.getBoolean(
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_KEY,
        QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_DEFAULT);
    return new QatDecompressor(bufferSize, useNativeBB, forcePinned, numa);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectDecompressor createDirectDecompressor() {
    return isNativeCodeLoaded() ? new QatDirectDecompressor() : null;
  }

  /**
   * Get the default filename extension for this kind of compression.
   *
   * @return <code>.qat</code>.
   */
  @Override
  public String getDefaultExtension() {
    return ".qat";
  }
}
