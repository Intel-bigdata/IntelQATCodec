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
package org.apache.hadoop.io.compress.conf;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.CommonConfigurationKeys;

@InterfaceAudience.Private
@InterfaceStability.Unstable
public class QatConfigurationKeys extends CommonConfigurationKeys {
  /** Internal buffer size for qat compressor/decompressors */
  public static final String IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY =
      "io.compression.codec.qat.buffersize";

  /** Default value for IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_KEY */
  public static final int IO_COMPRESSION_CODEC_QAT_BUFFERSIZE_DEFAULT =
      256 * 1024;
  
  /**
   * Whether to use native allocate BB for creating ByteBuffer.
   */
  public static final String IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_KEY =
      "io.compression.codec.qat.use-native-allocate-bb";

  public static final boolean IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_DEFAULT = false;
  
  /**
   * Whether to use force pinned for native allocate BB, it is applicable only
   * when IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_KEY is enabled.
   */
  public static final String IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_KEY =
      "io.compression.codec.qat.native-allocate-bb.force-pinned";

  public static final boolean IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_DEFAULT = true;

  /**
   * Whether to use numa for native allocate BB, it is applicable only when
   * IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_KEY is enabled.
   */
  public static final String IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_KEY =
      "io.compression.codec.qat.native-allocate-bb.numa";

  public static final boolean IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_DEFAULT = false;
}
