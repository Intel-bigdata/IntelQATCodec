#/**
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

# A function designed to check whether every environment variable in a list is
# defined.
check = $(foreach var,$(1),$(if $($(var)),,$(error Please set $(var))))

# These were the compiler flags used when the native part was built into the
# Hadoop native library. It has been decoupled since, so one or more of these
# flags might be redundant.
FLAGS = -g -Wall -O2 -D_REENTRANT -D_GNU_SOURCE -D_LARGEFILE_SOURCE \
		-D_FILE_OFFSET_BITS=64 \
		-fstack-protector-all  \
		-D_FORTIFY_SOURCE=2    \
		-Wformat -Wformat-security -Werror=format-security

INC = -I $(JAVA_HOME)/include \
      -I $(JAVA_HOME)/include/linux \
      -I $(QATZIPSRC)/include \
      -I javah \
      -I ../../src/main/native \
      -I .

libqatcodec.so: QatCompressorJNI.o QatDecompressorJNI.o util.o
	gcc -fpic -shared -o ../../target/libqatcodec.so -ldl QatCompressorJNI.o \
	  QatDecompressorJNI.o util.o

QatCompressorJNI.o: ../../src/main/native/QatCompressorJNI.c javah
	$(call check,JAVA_HOME )
	gcc -fpic $(FLAGS) $(INC) -c ../../src/main/native/QatCompressorJNI.c

QatDecompressorJNI.o: ../../src/main/native/QatDecompressorJNI.c javah
	$(call check,JAVA_HOME )
	gcc -fpic $(FLAGS) $(INC) -c ../../src/main/native/QatDecompressorJNI.c

util.o: ../../src/main/native/util.c
	$(call check,JAVA_HOME )
	gcc -fpic $(FLAGS) $(INC) -c ../../src/main/native/util.c

javah:
	$(call check,JAVA_HOME)
	$(JAVA_HOME)/bin/javah -d javah -cp ../../src/main/java \
	  com.intel.qat.jni.QatCompressorJNI \
	  com.intel.qat.jni.QatDecompressorJNI

clean:
	rm -fr ../../target/libqatcodec.so QatCompressorJNI.o QatDecompressorJNI.o util.o \
	  javah
