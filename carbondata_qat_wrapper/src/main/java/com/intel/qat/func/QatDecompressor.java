package com.intel.qat.func;

import com.intel.qat.jni.CarbondataQatJNI;

import java.io.IOException;

public class QatDecompressor {

    public synchronized byte [] decompress(byte[] compInput) throws IOException {
        if (compInput == null) {
            throw new NullPointerException();
        }

        byte[] des = new byte[compInput.length*2];
        int uncompressedLen = CarbondataQatJNI.decompress(compInput, 0, compInput.length, des);

        byte[] res = new byte[uncompressedLen];
        System.arraycopy(des, 0, res, 0, uncompressedLen);
        return res;
    }

    public synchronized byte [] decompress(byte[] compInput, int srcOff, int srcLen) throws IOException{
        if (compInput == null) {
            throw new NullPointerException();
        }
        if (srcOff < 0 || srcLen < 0 || srcOff >= compInput.length) {
                throw new ArrayIndexOutOfBoundsException();
        }

        byte[] des = new byte[compInput.length*2];
        int uncompressedLen = CarbondataQatJNI.decompress(compInput, srcOff, srcLen, des);

        byte[] res = new byte[uncompressedLen];
        System.arraycopy(des, 0, res, 0, uncompressedLen);
        return res;
    }
}
