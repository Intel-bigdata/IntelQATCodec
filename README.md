# QAT Codec

QAT Codec project provides compression and decompression library for Apache
Hadoop to make use of the Intel® QuickAssist Technology for compression/decompression. 

Big data analytics are commonly performed on large data sets that are moved
within a Hadoop cluster containing high-volume, industry-standard servers.
A significant amount of time and network bandwidth can be saved when the data
is compressed before it is passed between servers, as long as the compression/
decompression operations are efficient and require negligible CPU cycles.
This is possible with the hardware-based compression delivered by Intel®
QuickAssist Technology, which is easy to integrate into existing systems
and networks using the available Intel drivers and patches.

## Online Documentation

http://www.intel.com/content/www/us/en/embedded/technology/quickassist/overview.html

## Building QAT Codec

### 1. Building with Maven

This option assumes that you have installed maven in your build machine. Also assumed to have java installed and set JAVA_HOME

Run the following command for building qatcodec.jar and libqatcodec.so

 mvn clean install -Dqatzip.libs=QATZIP_LIBRARIES_PATH -Dqatzip.src=QATZIP_SOURCE_CODE PATH

Here 
     
     qatzip.libs - A path where qatzip libraries placed. This is needed because QATCodec depends on qatzip libraries.
     qatzip.src  - A path where qatzip source code placed. This is needed because QATCodec needs qatzip exposed h files for building.

Native code building will be skipped in Windows machine as QATCodec native code can not be build in Windows.

When you run the build in Linux os, native code will be build automatically when run the above command.

If you want native building to be skipped in linux os explicitly, then you need to mention -DskipNative

 ex: mvn clean install -Dqatzip.libs=QATZIP_LIBRARIES_PATH -Dqatzip.src=QATZIP_SOURCE_CODE PATH -DskipNative

By default above commands will run the test cases as well. TO skip the test cases to run use the following command

 mvn clean install -DskipTests Dqatzip.libs=QATZIP_LIBRARIES_PATH -Dqatzip.src=QATZIP_SOURCE_CODE_PATH

To run the specific test cases

 mvn clean test -Dtest=TestQatCompressorDecompressor Dqatzip.libs=QATZIP_LIBRARIES_PATH -Dqatzip.src=QATZIP_SOURCE_CODE PATH


### 2. Building with Makefile
  
#### 1. Building qatcodec.jar
Set the below env variables,

  JAVA_HOME - Java home

  HADOOPJARS - Cloudera Hadoop jars

After exporting above parameters execute the following commands

  cd QATCodec/build/

  make


#### 2. Building libqatcodec.so

Set the below env variables,

  JAVA_HOME - Java home

  QATZIPSRC - QATZIP source code path

  LD_LIBRARY_PATH - make sure to export LD_LIBRARY_PATH with qatzip libraries

After exporting above parameters execute the following commands

  cd QATCodec/build/native/

  make


#### For any security concerns, please visit https://01.org/security.