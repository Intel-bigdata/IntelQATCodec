package com.intel.qat.jni;

import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


import java.io.IOException;

class QatDecompressorJNITest {

    //@org.junit.jupiter.api.Test
    @Test
    void isNativeCodeLoaded() {
    }

   @Test
    void setInput() {
    }

    @Test
    void decompress() throws IOException {
        for(int i = 0; i < 3; i++){
            final byte[] decompressed = new byte[]{1,2,3,4};
            final int off = 1;
            final int len = 3;
            final byte[] compressed = new byte[]{37,31,-117,8,4,0,0,0,0,0,-1,12,0,81,90,8,0,0,0,0,0,0,0,0,0,99,98,102,1,0,-90,-102,-123,-48,3,0,0,0};
            QatDecompressorJNI qatDecompressor = new QatDecompressorJNI();
            qatDecompressor.reset();
            qatDecompressor.setInput(compressed, 0,38 );
            byte[] bytes = new byte[3];
            int length = qatDecompressor.decompress(bytes,0,3);
            System.out.println("the decompressed length is : "+ length);

        }
    }

    @Test
    void reset() {
    }

    @Test
    void end() {
    }
}