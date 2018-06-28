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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * Decompresses the compressed data <compressedDataPath> using the <codec> with
 * one thread for each file decompression.
 *
 * Usage : org.apache.hadoop.util.benchmark.DataDecompressor <codec>
 * <compressedDataPath> <actualDatapath>
 */
public class DataDecompressor {

  public static void main(String[] args)
      throws ClassNotFoundException, IOException, InterruptedException {
    if (args.length != 3) {
      System.out
          .println("Usage : org.apache.hadoop.util.benchmark.DataDecompressor"
              + " <codec> <compressedDataPath> <actualDatapath>");
      System.exit(-1);
    }
    System.out.println("Start Time : " + new Date());
    long startTime = System.currentTimeMillis();

    String codecStr = args[0];
    String outputPath = args[1];
    String actualDataPath = args[2];
    Configuration conf = new Configuration();
    Class<?> codecClass = Class.forName(codecStr);

    File file = new File(outputPath);
    if (!file.exists()) {
      throw new IOException("Dir doesn't exist : " + outputPath);
    } else if (!file.isDirectory()) {
      throw new IOException(outputPath + " is not a directory.");
    }
    final CompressionCodec codec = (CompressionCodec) ReflectionUtils
        .newInstance(codecClass, conf);

    String[] dirFiles = file.list();
    if (dirFiles == null) {
      throw new IOException("There are no files in the " + outputPath);
    }

    File actulaDatafile = new File(actualDataPath);
    if (actulaDatafile.exists()) {
      throw new IOException("File/Dir already exists : " + actulaDatafile);
    } else if (!actulaDatafile.mkdirs()) {
      throw new IOException(
          "Failed to create the directory : " + actulaDatafile);
    }

    CountDownLatch countDownLatch = new CountDownLatch(dirFiles.length);
    CyclicBarrier cyclicBarrier = new CyclicBarrier(dirFiles.length);

    for (String fileName : dirFiles) {
      createAndStartThread(outputPath + File.separator + fileName,
          countDownLatch, cyclicBarrier, codec,
          actulaDatafile + File.separator + fileName);
    }
    countDownLatch.await();
    long finishedTime = System.currentTimeMillis();
    System.out.println("Finished Time : " + new Date());
    System.out
        .println("Total time Taken (millis) : " + (finishedTime - startTime));
  }

  private static Thread createAndStartThread(final String outputPath,
      final CountDownLatch countDownLatch, final CyclicBarrier cyclicBarrier,
      final CompressionCodec codec, final String actualFilePath) {
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          cyclicBarrier.await();
          executeTask(outputPath, codec, actualFilePath);
          countDownLatch.countDown();
        } catch (Exception e) {
          System.err.println("Failed while executing the task.");
          e.printStackTrace();
          System.exit(-1);
        }
      }

      private void executeTask(String compressedFilePath,
          CompressionCodec codec, String actualDataFilePath) {
        FileInputStream fileInputStream = null;
        CompressionInputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        boolean exit = false;
        try {
          fileInputStream = new FileInputStream(compressedFilePath);
          inputStream = codec.createInputStream(fileInputStream);

          fileOutputStream = new FileOutputStream(actualDataFilePath);
          byte[] buffer = new byte[1024];
          int length;
          while ((length = inputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
          }
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(-1);
        } finally {
          if (fileOutputStream != null) {
            try {
              fileOutputStream.close();
            } catch (IOException e) {
              e.printStackTrace();
              exit = true;
            }
          }
          if (inputStream != null) {
            try {
              inputStream.close();
            } catch (IOException e) {
              e.printStackTrace();
              exit = true;
            }
          }
          if (fileInputStream != null) {
            try {
              fileInputStream.close();
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
