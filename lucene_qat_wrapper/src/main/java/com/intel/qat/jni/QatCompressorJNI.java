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


package com.intel.qat.jni;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


import com.intel.qat.conf.QatConfigurationKeys;
import com.intel.qat.util.QatNativeCodeLoader;


/**
 * @author root
 * @date 12/3/19 12:39 PM
 * @decsription: A {@link Compressor} based on the qat compression algorithm.
 * @link qat-parent
 */

public class QatCompressorJNI {

    private static final Logger LOG = LogManager.getLogger(QatCompressorJNI.class.getName());
    private static final int DEFAULT_DIRECT_BUFFER_SIZE = 64 * 1024;

    // HACK - Use this as a global lock in the JNI layer
    @SuppressWarnings({"rawtypes"})
    private static Class clazz = QatCompressorJNI.class;

    private int directBufferSize;
    private Buffer compressedDirectBuf = null;
    private int uncompressedDirectBufLen;
    private Buffer uncompressedDirectBuf = null;
    private byte[] userBuf = null;
    private int userBufOff = 0, userBufLen = 0;
    private boolean finish, finished;

    private long bytesRead = 0L;
    private long bytesWritten = 0L;

    private static boolean nativeQatLoaded = false;

    static {
        if (QatNativeCodeLoader.isNativeCodeLoaded() &&
                QatNativeCodeLoader.buildSupportsQat()) {
            System.out.println("-------->the library name is " + QatNativeCodeLoader.getLibraryName());
            try {
                String value = System.getProperty("QAT_COMPRESS_LEVEL");
                int level = 1;
                if (value != null) {
                    try {
                        level = Integer.parseInt(value);
                        if (level < 1 || level > 9) {
                            level = 1;
                            LOG.warn("Invalid value for compression level:" + value
                                    + ", value should be in range 1-9."
                                    + " Proceeding with default value as 1.");
                        }
                    } catch (NumberFormatException e) {
                        level = 1;
                        LOG.warn("Could not parse the value:" + value
                                + ", compression level should be in range 1-9."
                                + " Proceeding with default value as 1.");
                    }
                }
                initIDs(level);
                nativeQatLoaded = true;
            } catch (Throwable t) {
                LOG.error("failed to load QatCompressor AMAC QatCompressor", t);
            }
        }
    }

    public static boolean isNativeCodeLoaded() {
        return nativeQatLoaded;
    }

    /**
     * Creates a new compressor.
     *
     * @param directBufferSize size of the direct buffer to be used.
     * @param numa
     * @param forcePinned
     */
    public QatCompressorJNI(int directBufferSize, boolean useNativeAllocateBB,
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
        compressedDirectBuf.position(directBufferSize);
    }

    /**
     * Creates a new compressor with the directBufferSize.
     *
     * @param directBufferSize
     */
    public QatCompressorJNI(int directBufferSize) {
        this(directBufferSize,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_USE_NATIVE_ALLOCATE_BB_DEFAULT,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_FORCE_PINNED_DEFAULT,
                QatConfigurationKeys.IO_COMPRESSION_CODEC_QAT_NATIVE_ALLOCATE_BB_NUMA_DEFAULT);
    }

    /**
     * Creates a new compressor with the default buffer size.
     */
    public QatCompressorJNI() {
        this(DEFAULT_DIRECT_BUFFER_SIZE);
    }

    /**
     * Sets input data for compression.
     * This should be called whenever #needsInput() returns
     * <code>true</code> indicating that more input data is required.
     *
     * @param b   Input data
     * @param off Start offset
     * @param len Length
     */

    public synchronized void setInput(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        finished = false;

        if (len > uncompressedDirectBuf.remaining()) {
            // save data; now !needsInput
            this.userBuf = b;
            this.userBufOff = off;
            this.userBufLen = len;
        } else {
            ((ByteBuffer) uncompressedDirectBuf).put(b, off, len);
            uncompressedDirectBufLen = uncompressedDirectBuf.position();
        }

        bytesRead += len;
    }

    /**
     * If a write would exceed the capacity of the direct buffers, it is set
     * aside to be loaded by this function while the compressed data are
     * consumed.
     */
    synchronized void setInputFromSavedData() {
        if (0 >= userBufLen) {
            return;
        }
        finished = false;

        uncompressedDirectBufLen = Math.min(userBufLen, directBufferSize);
        ((ByteBuffer) uncompressedDirectBuf).put(userBuf, userBufOff,
                uncompressedDirectBufLen);

        // Note how much data is being fed to qat
        userBufOff += uncompressedDirectBufLen;
        userBufLen -= uncompressedDirectBufLen;
    }

    /**
     * Does nothing.
     */

    public synchronized void setDictionary(byte[] b, int off, int len) {
        // do nothing
    }

    /**
     * Returns true if the input data buffer is empty and
     * #setInput() should be called to provide more input.
     *
     * @return <code>true</code> if the input data buffer is empty and
     *         #setInput() should be called in order to provide more input.
     */

    public synchronized boolean needsInput() {
        return !(compressedDirectBuf.remaining() > 0
                || uncompressedDirectBuf.remaining() == 0 || userBufLen > 0);
    }

    /**
     * When called, indicates that compression should end
     * with the current contents of the input buffer.
     */

    public synchronized void finish() {
        finish = true;
    }

    /**
     * Returns true if the end of the compressed
     * data output stream has been reached.
     *
     * @return <code>true</code> if the end of the compressed
     *         data output stream has been reached.
     */

    public synchronized boolean finished() {
        // Check if all uncompressed data has been consumed
        return (finish && finished && compressedDirectBuf.remaining() == 0);
    }

    /**
     * Fills specified buffer with compressed data. Returns actual number
     * of bytes of compressed data. A return value of 0 indicates that
     * needsInput() should be called in order to determine if more input
     * data is required.
     *
     * @param b   Buffer for the compressed data
     * @param off Start offset of the data
     * @param len Size of the buffer
     * @return The actual number of bytes of compressed data.
     */

    public synchronized int compress(byte[] b, int off, int len)
            throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }

        // Check if there is compressed data
        int n = compressedDirectBuf.remaining();
        if (n > 0) {
            n = Math.min(n, len);
            ((ByteBuffer) compressedDirectBuf).get(b, off, n);
            bytesWritten += n;
            return n;
        }

        // Re-initialize the qat's output direct-buffer
        compressedDirectBuf.clear();
        compressedDirectBuf.limit(0);
        if (0 == uncompressedDirectBuf.position()) {
            // No compressed data, so we should have !needsInput or !finished
            setInputFromSavedData();
            if (0 == uncompressedDirectBuf.position()) {
                // Called without data; write nothing
                finished = true;
                return 0;
            }
        }

        // Compress data
        n = compressBytesDirect();
        compressedDirectBuf.limit(n);
        uncompressedDirectBuf.clear(); // qat consumes all buffer input

        // Set 'finished' if qat has consumed all user-data
        if (0 == userBufLen) {
            finished = true;
        }

        // Get atmost 'len' bytes
        n = Math.min(n, len);
        bytesWritten += n;
        ((ByteBuffer) compressedDirectBuf).get(b, off, n);

        return n;
    }

    /**
     * Resets compressor so that a new set of input data can be processed.
     */

    public synchronized void reset() {
        finish = false;
        finished = false;
        uncompressedDirectBuf.clear();
        uncompressedDirectBufLen = 0;
        compressedDirectBuf.clear();
        compressedDirectBuf.limit(0);
        userBufOff = userBufLen = 0;
        bytesRead = bytesWritten = 0L;
    }

    /**
     * Return number of bytes given to this compressor since last reset.
     */

    public synchronized long getBytesRead() {
        return bytesRead;
    }

    /**
     * Return number of bytes consumed by callers of compress since last reset.
     */

    public synchronized long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * Closes the compressor and discards any unprocessed input.
     */

    public synchronized void end() {
    }

    private native static void initIDs(int level);

    private native int compressBytesDirect();

    public native static String getLibraryName();

    public native Object nativeAllocateBB(long capacity, boolean numa,
                                          boolean forcePinned);
}
