package com.intel.qat;

import com.intel.qat.func.QatCompressor;
import com.intel.qat.func.QatDecompressor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Random;

public class TestQatCompressorDecompressor {
    QatCompressor compressor = new QatCompressor();
    QatDecompressor decompressor = new QatDecompressor();

    @Test
    public void testQatCompressorSingleInputNullPointerException(){

        try{
            compressor.compress(null);
            fail("testQatCompressorSingleInputNullPointerException error!");
        } catch (NullPointerException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatCompressorSingleInputNullPointerException ex error!");
        }
    }

    @Test
    public void testQatCompressorMultiInputNullPointerException(){

        try{
            compressor.compress(null, 0);
            fail("testQatCompressorMultiInputNullPointerException error!");
        } catch (NullPointerException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatCompressorMultiInputNullPointerException ex error!");
        }
    }

    @Test
    public void testQatDecompressorSingleInputNullPointerException(){

        try{
            decompressor.decompress(null);
            fail("testQatDecompressorSingleInputNullPointerException error!");
        } catch (NullPointerException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatDecompressorSingleInputNullPointerException ex error!");
        }
    }

    @Test
    public void testQatDecompressorMultiInputNullPointerException(){

        try{
            decompressor.decompress(null, 0, 0);
            fail("testQatDecompressorMultiInputNullPointerException error!");
        } catch (NullPointerException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatDecompressorMultiInputNullPointerException ex error!");
        }
    }

    @Test
    public void testQatCompressorAIOBException(){

        try{
            compressor.compress(new byte[]{}, -1);
            fail("testQatCompressorAIOBException error!");
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatCompressorAIOBException ex error!");
        }
    }

    @Test
    public void testQatDecompressorAIOBException(){

        try{
            decompressor.decompress(new byte[]{}, -1, 0);
            fail("testQatDecompressorAIOBException error!");
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        } catch (Exception e) {
            fail("testQatDecompressorAIOBException ex error!");
        }
    }


    @Test
    public void testQatCompressDecompressSingleInput() {

        int BYTE_SIZE = 1024*54;
        byte [] unComp1 = BytesGenerator.get(BYTE_SIZE);

        try {
            byte [] comp = compressor.compress(unComp1);
            byte [] unComp2 = decompressor.decompress(comp);
            Assert.assertArrayEquals(unComp1, unComp2);
        } catch (Exception e){
            fail("testQatCompressDecompressSingleInput error!" + e.getMessage());
        }
    }

    @Test
    public void testQatCompressDecompressMultiInput() {

        int BYTE_SIZE = 1024*45;
        byte [] unComp1 = BytesGenerator.get(BYTE_SIZE);

        try {
            byte [] comp = compressor.compress(unComp1, BYTE_SIZE);
            byte [] unComp2 = decompressor.decompress(comp, 0, comp.length);
            Assert.assertArrayEquals(unComp1, unComp2);
        } catch (Exception e){
            fail("testQatCompressDecompressMultiInput error!" + e.getMessage());
        }
    }


    static final class BytesGenerator {
        private BytesGenerator() {
        }

        private static final byte[] CACHE = new byte[]{0x0, 0x1, 0x2, 0x3, 0x4, 0x5,
                0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};
        private static final Random rnd = new Random(12345l);

        public static byte[] get(int size) {
            byte[] array = (byte[]) Array.newInstance(byte.class, size);
            for (int i = 0; i < size; i++)
                array[i] = CACHE[rnd.nextInt(CACHE.length - 1)];
            return array;
        }
    }

}
