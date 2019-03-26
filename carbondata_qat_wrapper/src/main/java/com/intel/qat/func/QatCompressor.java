package com.intel.qat.func;

import com.intel.qat.jni.CarbondataQatJNI;

import java.io.IOException;


public class QatCompressor {

    public synchronized byte [] compress(byte[] unCompInput) throws IOException {
        if (unCompInput == null) {
            throw new NullPointerException();
        }

        int maxDesLen = CarbondataQatJNI.maxCompressedLength(unCompInput.length);
        byte[] des = new byte[maxDesLen];
        int compressedLen = CarbondataQatJNI.compress(unCompInput, unCompInput.length, des);

        byte[] res = new byte[compressedLen];
        System.arraycopy(des, 0, res, 0, compressedLen);
        return res;
    }

    public synchronized byte [] compress(byte[] unCompInput, int srcLen) throws IOException {
        if (unCompInput == null) {
            throw new NullPointerException();
        }
        if (srcLen < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }

        srcLen = Math.min(srcLen, unCompInput.length);

        int maxDesLen = CarbondataQatJNI.maxCompressedLength(srcLen);
        byte[] des = new byte[maxDesLen];
        int compressedLen = CarbondataQatJNI.compress(unCompInput, srcLen, des);

        byte[] res = new byte[compressedLen];
        System.arraycopy(des, 0, res, 0, compressedLen);
        return res;
    }

}
