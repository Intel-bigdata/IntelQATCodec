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

package com.intel.qat.es;


import com.intel.qat.jni.QatCodecJNI;
import com.intel.qat.util.buffer.BufferAllocator;
import com.intel.qat.util.buffer.CachedBufferAllocator;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * This class implements a stream filter for uncompressing data in the
 * "qat" compression format.
 */
public class QatCompressionInputStream extends FilterInputStream {

    private final BufferAllocator compressedBufferAllocator;
    private final BufferAllocator uncompressedBufferAllocator;
    private final ByteBuffer compressedBuffer;
    private final ByteBuffer uncompressedBuffer;
    private final BufferAllocator tempBufferAllocator;

    //Input buffer for decompression.
    protected byte[] buf;
    //Length of input buffer.
    protected int len;
    boolean useDefaultQatDecompressor = false;
    private boolean closed;
    // this flag is set to true after EOF has reached
    private boolean reachEOF;
    private long context;
    private int compressedSize;
    private int uncompressedSize;
    private int originalLen;
    private int uncompressedBufferPosition;
    private byte[] tempBuffer;
    private byte[] singleByteBuf = new byte[1];

    /**
     * Creates a new input stream with the specified decompressor and
     * buffer size.
     *
     * @param in              the input stream
     * @param size            the input buffer size
     * @param useNativeBuffer identify the buffer type
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    public QatCompressionInputStream(InputStream in, int size, boolean useNativeBuffer) {
        super(in);
        if (in == null) {
            throw new NullPointerException();
        } else if (size <= 0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }

        this.compressedSize = size * 3 / 2;
        this.uncompressedSize = size;

        // allocate the buffer
        this.uncompressedBufferAllocator = CachedBufferAllocator
                .getBufferAllocatorFactory().getBufferAllocator(uncompressedSize);
        this.compressedBufferAllocator = CachedBufferAllocator
                .getBufferAllocatorFactory().getBufferAllocator(compressedSize);
        this.uncompressedBuffer = uncompressedBufferAllocator
                .allocateDirectByteBuffer(useNativeBuffer, uncompressedSize, 64);
        this.compressedBuffer = compressedBufferAllocator
                .allocateDirectByteBuffer(useNativeBuffer, compressedSize, 64);

        if (null != uncompressedBuffer) {
            uncompressedBuffer.clear();
        }
        if (null != compressedBuffer) {
            compressedBuffer.clear();
        }

        this.uncompressedBufferPosition = 0;
        this.originalLen = 0;

        // cache compressed stream
        tempBufferAllocator = CachedBufferAllocator
                .getBufferAllocatorFactory().getBufferAllocator(compressedSize);
        tempBuffer = tempBufferAllocator
                .allocateByteArray(compressedSize);

        this.context = QatCodecJNI.createDecompressContext();
        buf = new byte[size];
        closed = false;
        reachEOF = false;

    }


    /**
     * Creates a new input stream with the specified buffer and a
     * default buffer size.
     *
     * @param in              the input stream
     * @param useNativeBuffer true if the buffer is native
     */
    public QatCompressionInputStream(InputStream in, boolean useNativeBuffer) {
        this(in, 512, useNativeBuffer);
    }

    /***
     * Creates a new input stream with a default buffer and buffer size.
     * @param in the input stream
     */
    public QatCompressionInputStream(InputStream in) {
        this(in, 512, true);
        useDefaultQatDecompressor = true;
    }

    /**
     * Check to make sure that this stream has not been closed
     */
    private void checkStream() throws IOException {
        if (context == 0) {
            throw new NullPointerException("This output stream's context is not initialized");
        }
        if (closed) {
            throw new IOException("stream closed");
        }
    }

    /**
     * @return 0 after EOF has been reached ,otherwise always return 1 (before EOF)
     * @throws IOException
     */
    @Override
    public int available() throws IOException {
        checkStream();
        return originalLen - uncompressedBufferPosition;
    }

    /**
     * Reads a byte of uncompressed data. This method will block until
     * enough input is available for decompression.
     *
     * @return the byte read, or -1 if end of compressed input is reached
     * @throws IOException if an I/O error has occurred
     */
    public int read() throws IOException {
        checkStream();
        return read(singleByteBuf, 0, 1) == -1 ? -1 : Byte.toUnsignedInt(singleByteBuf[0]);
    }


    /**
     * Reads uncompressed data into an array of bytes. If <code>len</code> is not
     * zero, the method will block until some input can be decompressed; otherwise,
     * no bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the
     * compressed input is reached
     */
    public int read(byte[] b, int off, int len) throws IOException {
        checkStream();
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new ArrayIndexOutOfBoundsException("BlockInputStream read requested lenght " + len
                    + " from offset " + off + " in buffer of size " + b.length);
        } else if (len == 0) {
            return 0;
        }

        if (uncompressedBufferPosition == originalLen) {
            fill();
        }
        if (reachEOF) {
            return -1;
        }

        len = Math.min(len, originalLen - uncompressedBufferPosition);

        uncompressedBuffer.get(b, off, len);
        uncompressedBufferPosition += len;
        return len;

    }

    private void fill() throws IOException {
        checkStream();
        int compressedLen = 0;
        try {
            compressedLen = readCompressedBlockLength();
        } catch (IOException e) {
            reachEOF = true;
            return;
        }

        if (compressedBuffer.capacity() < compressedLen) {
            throw new IOException("Input Stream is corrupted, compressed length large than " + compressedSize);
        }

        readCompressedData(compressedBuffer, compressedLen);

        try {
            final int uncompressed_size = QatCodecJNI.decompress(context, compressedBuffer, 0, compressedLen, uncompressedBuffer, 0, uncompressedSize);
            originalLen = uncompressed_size;
        } catch (RuntimeException e) { //  need change
            throw new IOException("Input Stream is corrupted, can't decompress", e);
        }

        uncompressedBuffer.position(0);
        uncompressedBuffer.limit(originalLen);
        uncompressedBufferPosition = 0;

    }

    private int readCompressedBlockLength() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException();
        return ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
    }

    private void readCompressedData(ByteBuffer bf, int len) throws IOException {
        int read = 0;
        assert bf.capacity() >= len;
        bf.clear();

        while (read < len) {
            final int bytesToRead = Math.min(len - read, tempBuffer.length);
            final int r = in.read(tempBuffer, 0, bytesToRead);
            if (r < 0) {
                throw new EOFException("Unexpected end of input stream");
            }
            read += r;
            bf.put(tempBuffer, 0, r);
        }

        bf.flip();
    }


    /**
     * Skips specified number of bytes of uncompressed data.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped.
     * @throws IOException              if an I/O error has occurred
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }

        checkStream();

        if (uncompressedBufferPosition == originalLen) {
            fill();
        }
        if (reachEOF) {
            return -1;
        }

        final int skipped = (int) Math.min(n, originalLen - uncompressedBufferPosition);
        uncompressedBufferPosition += skipped;
        uncompressedBuffer.position(uncompressedBufferPosition);

        return skipped;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            in.close();
        } finally {
            uncompressedBufferAllocator.releaseDirectByteBuffer(uncompressedBuffer);
            compressedBufferAllocator.releaseDirectByteBuffer(compressedBuffer);
            tempBufferAllocator.releaseByteArray(tempBuffer);
            tempBuffer = null;
            in = null;
            QatCodecJNI.destroyContext(context);
            context = 0;
            closed = true;
        }
    }

    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. The <code>markSupported</code>
     * method returns <code>false</code>.
     *
     * @return a <code>boolean</code> indicating if this stream type supports
     * the <code>mark</code> and <code>reset</code> methods.
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#reset()
     */

    public boolean markSupported() {
        return false;
    }

    @SuppressWarnings("sync-override")
    @Override
    public synchronized void mark(int readlimit) {

    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     *
     * <p> The method <code>reset</code> does nothing except throw an
     * <code>IOException</code>.
     *
     * @throws IOException if this method is invoked.
     * @see java.io.InputStream#mark(int)
     * @see java.io.IOException
     */
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
