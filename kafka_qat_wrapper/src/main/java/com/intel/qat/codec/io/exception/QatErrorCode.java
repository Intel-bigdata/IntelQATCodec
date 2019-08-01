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

package com.intel.qat.codec.io.exception;

/**
 * Kafka QAT error codes.
 */
public enum QatErrorCode {
  UNKNOWN(0),
  FAILED_TO_LOAD_NATIVE_LIBRARY(1),
  PARSING_ERROR(2),
  NOT_A_DIRECT_BUFFER(3),
  OUT_OF_MEMORY(4),
  FAILED_TO_UNCOMPRESS(5),
  EMPTY_INPUT(6),
  INCOMPATIBLE_VERSION(7),
  INVALID_CHUNK_SIZE(8);

  private final int id;

  QatErrorCode(int id) {
    this.id = id;
  }

  public static QatErrorCode getErrorCode(int id) {
    for (QatErrorCode code : QatErrorCode.values()) {
      if (code.id == id) {
        return code;
      }
    }
    return UNKNOWN;
  }

  public static String getErrorMessage(int id) {
    return getErrorCode(id).name();
  }
}
