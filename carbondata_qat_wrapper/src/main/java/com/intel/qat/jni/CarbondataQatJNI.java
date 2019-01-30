package com.intel.qat.jni;

public class CarbondataQatJNI {
    
    static{
        System.loadLibrary("CarbondataQatJNI");
        init();
    }

    public static native void init();

    public static native int compress(byte[] src, int srcLen, byte[] des);

    public static native int decompress(byte[] src, int srcOff, int srcLen, byte[] des);

    public static native int maxCompressedLength(int srcLen);
}
