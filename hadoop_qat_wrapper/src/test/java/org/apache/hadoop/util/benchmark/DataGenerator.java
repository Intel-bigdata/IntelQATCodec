/*
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
package org.apache.hadoop.util.benchmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * Generates compressed data using the <codec> for compression with
 * <numberoffiles> threads and each file size as <eachfilesize>.
 *
 * Usage : org.apache.hadoop.util.benchmark.DataGenerator <codec>
 * <numberoffiles> <eachfilesize> <outputpath>
 * 
 */
public class DataGenerator {

  public static void main(String[] args)
      throws ClassNotFoundException, InterruptedException, IOException {
    if (args.length != 4) {
      System.out
          .println("Usage : org.apache.hadoop.util.benchmark.DataGenerator"
              + " <codec> <numberoffiles> <eachfilesize> <outputpath>");
      System.exit(-1);
    }
    System.out.println("Start Time : " + new Date());
    long startTime = System.currentTimeMillis();

    String codecStr = args[0];
    int noOfFiles = Integer.parseInt(args[1]);
    long eachFileSize = Long.parseLong(args[2]);
    String outputPath = args[3];

    Configuration conf = new Configuration();
    Class<?> codecClass = Class.forName(codecStr);
    SecureRandom random = new SecureRandom();
    CountDownLatch countDownLatch = new CountDownLatch(noOfFiles);
    CyclicBarrier cyclicBarrier = new CyclicBarrier(noOfFiles);

    File file = new File(outputPath);
    if (file.exists()) {
      throw new IOException("File/Dir already exists : " + outputPath);
    } else if (!file.mkdirs()) {
      throw new IOException("Failed to create the directory : " + outputPath);
    }
    final CompressionCodec codec = (CompressionCodec) ReflectionUtils
        .newInstance(codecClass, conf);
    for (int i = 0; i < noOfFiles; i++) {
      createAndStartThread(eachFileSize, outputPath, random, countDownLatch,
          cyclicBarrier, codec, i);
    }
    countDownLatch.await();
    long finishedTime = System.currentTimeMillis();
    System.out.println("Finished Time : " + new Date());
    System.out
        .println("Total time Taken (millis) : " + (finishedTime - startTime));
  }

  private static Thread createAndStartThread(final long eachFileSize,
      final String outputPath, final SecureRandom random,
      final CountDownLatch countDownLatch, final CyclicBarrier cyclicBarrier,
      final CompressionCodec codec, final int count) {
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          cyclicBarrier.await();
          executeTask(eachFileSize, outputPath, random, codec, count);
          countDownLatch.countDown();
        } catch (Exception e) {
          System.err.println("Failed while executing the task.");
          e.printStackTrace();
          System.exit(-1);
        }
      }

      private void executeTask(final long eachFileSize, final String outputPath,
          final SecureRandom random, final CompressionCodec codec, int count) {
        FileOutputStream fileOutputStream = null;
        CompressionOutputStream codecOutputStream = null;
        boolean exit = false;
        try {
          fileOutputStream = new FileOutputStream(
              outputPath + File.separator + "data-" + count);
          codecOutputStream = codec.createOutputStream(fileOutputStream);
          for (long j = 0; j < eachFileSize; j = j + 26) {
            byte[] bytes = new BigInteger(130, random).toString(32)
              .getBytes(Charset.defaultCharset().name());
            codecOutputStream.write(bytes);
          }
          codecOutputStream.flush();
          fileOutputStream.flush();
        } catch (IOException e) {
          e.printStackTrace();
          exit = true;
        } finally {
          if (codecOutputStream != null) {
            try {
              codecOutputStream.close();
            } catch (IOException e) {
              e.printStackTrace();
              exit = true;
            }
          }
          if (fileOutputStream != null) {
            try {
              fileOutputStream.close();
            } catch (IOException e) {
              e.printStackTrace();
              exit = true;
            }
          }
        }
        if (exit) {
          System.exit(-1);
        }
      }
    };
    thread.start();
    return thread;
  }
}
