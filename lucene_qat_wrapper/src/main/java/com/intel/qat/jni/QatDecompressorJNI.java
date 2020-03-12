/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.qat.jni;

import com.intel.qat.conf.QatConfigurationKeys;
import com.intel.qat.util.QatNativeCodeLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * @author root
 * @date 12/3/19 12:47 PM
 * @decsription: A {@link Decompressor} based on the qat compression algorithm. It's not thread-safe.
 * @link qat-parent
 */
public class QatDecompressorJNI {
    private static final Logger LOG = LogManager.getLogger(QatDecompressorJNI.class);

    private static final int DEFAULT_DIRECT_BUFFER_SIZE = 640 * 1024;

    // HACK - Use this as a global lock in the JNI layer
    @SuppressWarnings({"rawtypes"})
    private static Class clazz = QatDecompressorJNI.class;
    private static boolean nativeQatLoaded = false;

    static {
        if (QatNativeCodeLoader.isNativeCodeLoaded() &&
                QatNativeCodeLoader.buildSupportsQat()) {
            try {
                initIDs();
                nativeQatLoaded = true;
            } catch (Throwable t) {
                LOG.error("failed to load QatDecompressor", t);
            }
        }
    }

    private int directBufferSize;
    private Buffer compressedDirectBuf = null;
    private int compressedDirectBufLen;
    private Buffer uncompressedDirectBuf = null;
    private byte[] userBuf = null;
    private int userBufOff = 0, userBufLen = 0;
    private boolean finished;

    /**
     * Creates a new decompressor.
     *
     * @param directBufferSize    size of the direct buffer to be used.
     * @param useNativeAllocateBB
     * @param forcePinned
     * @param numa
     */
    public QatDecompressorJNI(int directBufferSize, boolean useNativeAllocateBB,
                              boolean forcePinned, boolean numa) {
        this.directBufferSize = directBufferSize;
        if (useNativeAllocateBB) {
            LOG.info("Creating ByteBuffer's using nativeAllocateBB.");
            try {
                uncompressedDirectBuf = (ByteBuffer) nativeAllocateBB(directBufferSize,
                        numa, forcePinned);
            } catch (Throwable t) {
                LOG.error("Failed to create ByteBuffer using nativeAllocateBB"
                        + " for uncompressed direct ByteBuffer. Creating the uncompressed"
                        + " ByteBuffer using ByteBuffer.allocateDirect().", t);
                uncompressedDirectBuf = ByteBuffer.allocateDirect(directBufferSize);
            }
            try {
                compressedDirectBuf = (ByteBuffer) nativeAllocateBB(directBufferSize,
                        numa, forcePinned);
            } catch (Throwable t) {
                LOG.error("Failed to create ByteBuffer using nativeAllocateBB"
                        + " for compressed direct ByteBuffer. Creating the compressed"
                        + " ByteBuffer using ByteBuffer.allocateDirect().", t);
                compressedDirectBuf = ByteBuffer.allocateDirect(directBufferSize);
            }
        } else {
            uncompressedDirectBuf = ByteBuffer.allocateDirect(directBufferSize);
            compressedDirectBuf = ByteBuffer.allocateDirect(directBufferSize);
        }
        uncompressedDirectBuf.position(directBufferSize);
    }

    /**
     * Creates a new decompressor with the directBufferSize.
     *
     * @param directBufferSize
     */
    public QatDecompressorJNI(int directBufferSize) {
        this(directBufferSize,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_DEFAULT,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_DEFAULT,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_DEFAULT);
    }

    /**
     * Creates a new decompressor with the default buffer size.
     */
    public QatDecompressorJNI() {
        this(DEFAULT_DIRECT_BUFFER_SIZE);
    }

    public static boolean isNativeCodeLoaded() {
        return nativeQatLoaded;
    }

    private native static void initIDs();

    /**
     * Sets input data for decompression.
     * This should be called if and only if {@link #needsInput()} returns
     * <code>true</code> indicating that more input data is required.
     * (Both native and non-native versions of various Decompressors require
     * that the data passed in via <code>b[]</code> remain unmodified until
     * the caller is explicitly notified--via {@link #needsInput()}--that the
     * buffer may be safely modified.  With this requirement, an extra
     * buffer-copy can be avoided.)
     *
     * @param b   Input data
     * @param off Start offset
     * @param len Length
     */

    public void setInput(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }

        this.userBuf = b;
        this.userBufOff = off;
        this.userBufLen = len;

        setInputFromSavedData();

        // Reinitialize qat's output direct-buffer
        uncompressedDirectBuf.limit(directBufferSize);
        uncompressedDirectBuf.position(directBufferSize);
    }

    /**
     * If a write would exceed the capacity of the direct buffers, it is set
     * aside to be loaded by this function while the compressed data are
     * consumed.
     */
    void setInputFromSavedData() {
        compressedDirectBufLen = Math.min(userBufLen, directBufferSize);

        // Reinitialize qat's input direct buffer
        compressedDirectBuf.rewind();
        ((ByteBuffer) compressedDirectBuf).put(userBuf, userBufOff,
                compressedDirectBufLen);

        // Note how much data is being fed to qat
        userBufOff += compressedDirectBufLen;
        userBufLen -= compressedDirectBufLen;
    }

    /**
     * Does nothing.
     */

    public void setDictionary(byte[] b, int off, int len) {
        // do nothing
    }

    /**
     * Returns true if the input data buffer is empty and
     * {@link #setInput(byte[], int, int)} should be called to
     * provide more input.
     *
     * @return <code>true</code> if the input data buffer is empty and
     * {@link #setInput(byte[], int, int)} should be called in
     * order to provide more input.
     */

    public boolean needsInput() {
        // Consume remaining compressed data?
        if (uncompressedDirectBuf.remaining() > 0) {
            return false;
        }

        // Check if qat has consumed all input
        if (compressedDirectBufLen <= 0) {
            // Check if we have consumed all user-input
            if (userBufLen <= 0) {
                return true;
            } else {
                setInputFromSavedData();
            }
        }

        return false;
    }

    /**
     * Returns <code>false</code>.
     *
     * @return <code>false</code>.
     */

    public boolean needsDictionary() {
        return false;
    }

    /**
     * Returns true if the end of the decompressed
     * data output stream has been reached.
     */

    public boolean finished() {
        return (finished && uncompressedDirectBuf.remaining() == 0);
    }

    /**
     * Fills specified buffer with uncompressed data. Returns actual number
     * of bytes of uncompressed data. A return value of 0 indicates that
     * {@link #needsInput()} should be called in order to determine if more
     * input data is required.
     *
     * @param b   Buffer for the compressed data
     * @param off Start offset of the data
     * @param len Size of the buffer
     * @return The actual number of bytes of compressed data.
     * @throws IOException
     */

    public int decompress(byte[] b, int off, int len)
            throws IOException {

        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }

        int n = 0;

        // Check if there is uncompressed data
        n = uncompressedDirectBuf.remaining();
        if (n > 0) {
            n = Math.min(n, len);
            ((ByteBuffer) uncompressedDirectBuf).get(b, off, n);
            return n;
        }
        if (compressedDirectBufLen > 0) {
            // Re-initialize the qat's output direct buffer
            uncompressedDirectBuf.rewind();
            uncompressedDirectBuf.limit(directBufferSize);

            // Decompress data
            n = decompressBytesDirect();

            uncompressedDirectBuf.limit(n);

            if (userBufLen <= 0) {
                finished = true;
            }

            // Get atmost 'len' bytes
            n = Math.min(n, len);
            ((ByteBuffer) uncompressedDirectBuf).get(b, off, n);

        }

        return n;
    }

    /**
     * Returns <code>0</code>.
     *
     * @return <code>0</code>.
     */

    public int getRemaining() {
        // Never use this function in BlockDecompressorStream.
        return 0;
    }

    public void reset() {
        finished = false;
        compressedDirectBufLen = 0;
        uncompressedDirectBuf.limit(directBufferSize);
        uncompressedDirectBuf.position(directBufferSize);
        userBufOff = userBufLen = 0;
    }

    /**
     * Resets decompressor and input and output buffers so that a new set of
     * input data can be processed.
     */
    public void end() {
        // do nothing
    }

    private native int decompressBytesDirect();

    public native Object nativeAllocateBB(long capacity, boolean numa,
                                          boolean forcePinned);

}
