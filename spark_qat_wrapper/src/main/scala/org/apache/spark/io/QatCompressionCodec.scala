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

package org.apache.spark.io

import java.io._

import com.intel.qat.spark._

import org.apache.spark.io._
import org.apache.spark.SparkConf

/**
 * QAT implementation of [[org.apache.spark.io.CompressionCodec]].
 * Block size can be configured by `spark.io.compression.qat.blockSize`.
 * To enable creating the ByteBuffer using malloc can be configured by
 * `spark.io.compression.qat.useNativeBuffer`.
 * The QAT compressor level can be configured by `spark.io.compression.qat.level`.
 *
 * @note The wire protocol for this codec is not guaranteed to be compatible across versions
 * of Spark. This is intended for use as an internal compression utility within a single Spark
 * application.
 */
class QatCompressionCodec(conf: SparkConf) extends CompressionCodec {

  override def compressedOutputStream(s: OutputStream): OutputStream = {
    val level = conf.getInt("spark.io.compression.qat.level", 1)
    val bufferSize = conf.getSizeAsBytes("spark.io.compression.qat.blockSize",
        "1024k").toInt
    val useNativeBuffer = conf.getBoolean("spark.io.compression.qat.useNativeBuffer",
        false)
    val useQzMalloc = conf.getBoolean("spark.io.compression.qat.native-bb.useQzMalloc",
        true)
    val useForcePinned = conf.getBoolean("spark.io.compression.qat.native-bb.useForcePinned",
        true)
    val useNuma = conf.getBoolean("spark.io.compression.qat.native-bb.useNuma",
        false)
    new QatCodecBlockOutputStream(s, level, bufferSize, useNativeBuffer, useQzMalloc,
        useForcePinned, useNuma)
  }

  override def compressedInputStream(s: InputStream): InputStream = {
    val bufferSize = conf.getSizeAsBytes("spark.io.compression.qat.blockSize",
        "1024k").toInt
    val useNativeBuffer = conf.getBoolean("spark.io.compression.qat.useNativeBuffer",
        false)
    val useQzMalloc = conf.getBoolean("spark.io.compression.qat.native-bb.useQzMalloc",
        true)
    val useForcePinned = conf.getBoolean("spark.io.compression.qat.native-bb.useForcePinned",
        true)
    val useNuma = conf.getBoolean("spark.io.compression.qat.native-bb.useNuma",
        false)
    new QatCodecBlockInputStream(s, bufferSize, useNativeBuffer, useQzMalloc, useForcePinned,
        useNuma)
  }
}
