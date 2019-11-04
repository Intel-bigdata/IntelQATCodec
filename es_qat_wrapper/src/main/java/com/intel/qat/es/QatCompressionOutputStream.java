package com.intel.qat.es;

import com.intel.qat.jni.QatCodecJNI;
import com.intel.qat.util.buffer.BufferAllocator;
import com.intel.qat.util.buffer.CachedBufferAllocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class QatCompressionOutputStream extends FilterOutputStream {
    private static final Logger LOG = LogManager.getLogger(QatCompressionOutputStream.class);
    private final boolean syncFlush;
    private final BufferAllocator tempBufferAllocator;
    protected byte[] buf;
    private long context;
    private int level;
    private int compressedSize;
    private int uncompressedSize;
    private BufferAllocator compressedBufferAllocator;
    private BufferAllocator uncompressedBufferAllocator;
    private ByteBuffer compressedBuffer;
    private ByteBuffer uncompressedBuffer;
    private boolean closed;
    private int uncompressedBufferPosition;
    private byte[] tempBuffer;

    /**
     * Create a new {@link OutputStream} with configurable codec level and block size. Large
     * blocks require more memory at compression and decompression time but
     * should improve the compression ratio.
     *
     * @param out             the {@link OutputStream} to feed
     * @param level           the compression codec level
     * @param size            the maximum number of bytes to try to compress at once,
     *                        must be >= 32 K
     * @param useNativeBuffer to identify if use nativeBuffer or not
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    public QatCompressionOutputStream(OutputStream out,
                                      int level, int size, boolean useNativeBuffer) {
        super(out);

        if (out == null) {
            throw new NullPointerException();
        } else if (size <= 0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }

        this.level = level;
        this.uncompressedSize = size;
        this.compressedSize = size * 3 / 2;
        this.uncompressedBufferAllocator = CachedBufferAllocator.
                getBufferAllocatorFactory().getBufferAllocator(uncompressedSize);
        this.compressedBufferAllocator = CachedBufferAllocator.
                getBufferAllocatorFactory().getBufferAllocator(compressedSize);
        this.uncompressedBuffer = uncompressedBufferAllocator.
                allocateDirectByteBuffer(useNativeBuffer, uncompressedSize, 64);
        this.compressedBuffer = compressedBufferAllocator.
                allocateDirectByteBuffer(useNativeBuffer, compressedSize, 64);
        if (uncompressedBuffer != null) {
            uncompressedBuffer.clear();
        }

        if (compressedBuffer != null) {
            compressedBuffer.clear();
        }

        uncompressedBufferPosition = 0;
        closed = false;

        tempBufferAllocator = CachedBufferAllocator.getBufferAllocatorFactory().
                getBufferAllocator(compressedSize);
        tempBuffer = tempBufferAllocator.allocateByteArray(compressedSize);

        context = QatCodecJNI.createCompressContext(level);
        LOG.debug("Create Qat OutputStream with level " + level);

        this.buf = new byte[size];
        this.syncFlush = false;
    }

    /**
     * Creates a new output stream with the default buffer.
     *
     * @param out   the output stream
     * @param level the compression codec level
     * @param size  the maximum number of bytes to try to compress at once,
     *              must be >= 32 K
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    public QatCompressionOutputStream(OutputStream out, int level, int size) {
        this(out, level, size, false);
    }

    /**
     * Creates a new output stream with the default size.
     *
     * @param out             the output stream
     * @param level           the compression codec level
     * @param useNativeBuffer to identify if use nativeBuffer or not
     * @throws IllegalArgumentException if {@code size <= 0}
     */

    public QatCompressionOutputStream(OutputStream out, int level, boolean useNativeBuffer) {
        this(out, level, 512, useNativeBuffer);
    }

    /**
     * Creates a new output stream with the default buffer size and level.
     * <p>The new output stream instance is created as if by invoking
     * the 3-argument constructor QatOutputStream(out,level,useNativeBuffer).
     *
     * @param out             the output stream
     * @param useNativeBuffer to identify the buffer
     */

    public QatCompressionOutputStream(OutputStream out, boolean useNativeBuffer) {
        this(out, 3, useNativeBuffer);
    }


    /**
     * Creates a new output stream with the default buffer and level.
     * <p>The new output stream instance is created as if by invoking
     * the 3-argument constructor QatOutputStream(out, level, size).
     *
     * @param out  the output stream
     * @param size the maximum number of bytes to try to compress at once,
     *             must be >= 32 K
     */
    public QatCompressionOutputStream(OutputStream out, int size) {
        this(out, 3, size);
    }

    /**
     * Creates a new output stream with the default buffer.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 2-argument constructor QatOutputStream(out, false).
     *
     * @param out the output stream
     */
    public QatCompressionOutputStream(OutputStream out) {
        this(out, false);
    }

    private static void WriteIntLE(int i, byte[] buf, int off) {
        buf[off] = (byte) i;
        buf[off + 1] = (byte) (i >>> 8);
        buf[off + 2] = (byte) (i >>> 16);
        buf[off + 3] = (byte) (i >>> 24);

    }

    private void checkStream() {
        if (context == 0) {
            throw new NullPointerException();
        }
        if (closed) {
            throw new IllegalStateException("The output stream has been closed");
        }
    }

    /**
     * Writes a byte to the compressed output stream. This method will
     * block until the byte can be written.
     *
     * @param b the byte to be written
     * @throws IOException if an I/O error has occurred
     */
    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) (b & 0xff);
        write(buf, 0, 1);
    }

    /**
     * Writes an array of bytes to the compressed output stream. This
     * method will block until all the bytes are written.
     *
     * @param b   the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     * @throws IOException if an I/O error has occurred
     */
    public void write(byte[] b, int off, int len) throws IOException {
        checkStream();

        if (b == null) {
            throw new NullPointerException();
        }

        if (len < 0 || off < 0 || len > b.length - off) {
            throw new ArrayIndexOutOfBoundsException("The output stream need length " + len + " from offset " + off + " in buffer of size " + b.length);
        }

        while (uncompressedBufferPosition + len > uncompressedSize) {
            int left = uncompressedSize - uncompressedBufferPosition;
            uncompressedBuffer.put(b, off, left);
            uncompressedBufferPosition = uncompressedSize;

            compressedBufferData();

            off += left;
            len -= left;

        }

        uncompressedBuffer.put(b, off, len);
        uncompressedBufferPosition += len;
        LOG.info("--> Writes bytes to the compressed(QAT) output stream");
    }

    private void compressedBufferData() throws IOException {
        if (uncompressedBufferPosition == 0) {
            return;
        }
        int compressedLen = QatCodecJNI.compress(context, uncompressedBuffer, 0, uncompressedBufferPosition, compressedBuffer, 0, compressedSize);

        WriteIntLE(compressedLen, tempBuffer, 0);

        compressedBuffer.position(0);
        compressedBuffer.limit(compressedLen);

        int totalWrite = 0;
        int off = 4;

        while (totalWrite < compressedLen) {
            int byteToWrite = Math.min((compressedLen - totalWrite), (tempBuffer.length - off));
            compressedBuffer.get(tempBuffer, off, byteToWrite);
            out.write(tempBuffer, 0, byteToWrite + off);
            totalWrite += byteToWrite;
            off = 0;
        }
        uncompressedBuffer.clear();
        compressedBuffer.clear();
        uncompressedBufferPosition = 0;
    }

    /**
     * Finishes writing compressed data to the output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    public void finish() throws IOException {
        checkStream();
        compressedBufferData();
        LOG.info("--> Finishes writing compressed(QAT) data to the output stream");
        out.flush();
    }

    /**
     * Writes remaining compressed data to the output stream and closes the
     * underlying stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            finish();
            out.close();
        } finally {
            closed = true;
            uncompressedBufferAllocator.releaseDirectByteBuffer(uncompressedBuffer);
            compressedBufferAllocator.releaseDirectByteBuffer(compressedBuffer);
            tempBufferAllocator.releaseByteArray(tempBuffer);
            tempBuffer = null;
            out = null;
            QatCodecJNI.destroyContext(context);
            context = 0;
            LOG.debug("Close Qat OutputStream with level " + level);
        }
    }

    public void flush() throws IOException {
        checkStream();
        compressedBufferData();
        LOG.info("--> Flush the data(QAT Compress)");
        out.flush();
    }
}
