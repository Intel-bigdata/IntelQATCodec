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
package org.apache.hadoop.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.util.ExitUtil.ExitException;
import org.junit.Test;

public class TestQatNativeLibraryChecker {

  @Test
  public void testNativeLibraryChecker() {
    ExitUtil.disableSystemExit();
    // help should return normally
    QatNativeLibraryChecker.main(new String[] { "-h" });
    // illegal arguments should exit
    expectExit(new String[] { "-a", "-h" });
    expectExit(new String[] { "aaa" });
    if (QatNativeCodeLoader.isNativeCodeLoaded()) {
      // no argument should return normally
      QatNativeLibraryChecker.main(new String[0]);
    } else {
      // no argument should exit
      expectExit(new String[0]);
    }
  }

  @Test
  public void testNativeLibraryCheckerOutput() {
    expectOutput(new String[] { "-a" });
    // no argument
    expectOutput(new String[0]);
  }

  private void expectExit(String[] args) {
    try {
      // should throw exit exception
      QatNativeLibraryChecker.main(args);
      fail("should call exit");
    } catch (ExitException e) {
      // pass
      ExitUtil.resetFirstExitException();
    }
  }

  private void expectOutput(String[] args) {
    ExitUtil.disableSystemExit();
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    PrintStream originalPs = System.out;
    String expectedStr = null;
    try {
      System.setOut(new PrintStream(outContent, false, "utf-8"));
      QatNativeLibraryChecker.main(args);
      expectedStr = outContent.toString("utf-8");
    } catch (ExitException e) {
      ExitUtil.resetFirstExitException();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } finally {
      if (Shell.WINDOWS) {
        assertEquals(expectedStr.indexOf("winutils: true") != -1, true);
      }
      if (QatNativeCodeLoader.isNativeCodeLoaded()) {
        assertEquals(expectedStr.indexOf("hadoop:  true") != -1, true);
      }
      System.setOut(originalPs);
    }
  }
}
