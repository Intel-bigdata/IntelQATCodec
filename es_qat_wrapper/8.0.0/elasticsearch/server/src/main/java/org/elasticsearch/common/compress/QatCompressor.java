/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.compress;

import com.intel.qat.es.QatCompressionInputStream;
import com.intel.qat.es.QatCompressionOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


public class QatCompressor implements Compressor {
    private static final byte[] HEADER = new byte[]{'Q', 'A', 'T', '\0'};
    private static final int LEVEL = 3;
    // limit the number of JNI calls
    private static final int BUFFER_SIZE = 4096;
    //add log to identify whether using qat
    private static final Logger logger = LogManager.getLogger(QatCompressor.class);

    @Override
    public boolean isCompressed(BytesReference bytes) {
        logger.debug("--> go into the isCompressed function");
        if (bytes.length() < HEADER.length) {
            return false;
        }
        for (int i = 0; i < HEADER.length; ++i) {
            if (bytes.get(i) != HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StreamInput streamInput(StreamInput in) throws IOException {
        logger.debug("--> go into the streamInput function");
        final byte[] headerBytes = new byte[HEADER.length];
        int len = 0;
        while (len < headerBytes.length) {
            final int read = in.read(headerBytes, len, headerBytes.length - len);
            if (read == -1) {
                break;
            }
            len += read;
        }
        if (len != HEADER.length || Arrays.equals(headerBytes, HEADER) == false) {
            throw new IllegalArgumentException("Input stream is not compressed with QAT!");
        }

        final boolean useNativeBuffer = false;

        QatCompressionInputStream qatInputStream = new QatCompressionInputStream(in, BUFFER_SIZE, useNativeBuffer);
        InputStream decompressedIn = new BufferedInputStream(qatInputStream, BUFFER_SIZE);

        return new InputStreamStreamInput(decompressedIn) {
            final AtomicBoolean closed = new AtomicBoolean(false);

            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    if (closed.compareAndSet(false, true)) {
                        // important to release memory
                        qatInputStream.close();
                    }
                }
            }
        };
    }

    @Override
    public StreamOutput streamOutput(StreamOutput out) throws IOException {
        logger.debug("--> go into the streamOutput function");
        out.writeBytes(HEADER);

        final boolean useNativeBuffer = false;
        QatCompressionOutputStream qatOutputStream = new QatCompressionOutputStream(out, LEVEL, BUFFER_SIZE, useNativeBuffer);
        OutputStream compressedOut = new BufferedOutputStream(qatOutputStream, BUFFER_SIZE);

        return new OutputStreamStreamOutput(compressedOut) {
            final AtomicBoolean closed = new AtomicBoolean(false);

            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    if (closed.compareAndSet(false, true)) {
                        // important to release memory
                        qatOutputStream.close();
                    }
                }
            }
        };
    }
}
