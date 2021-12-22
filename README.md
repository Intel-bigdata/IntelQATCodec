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

## Notice
[Intel QATCodec, version 2.3.0] has been updated to include functional and security updates. Users should update to the latest version.

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
  
## Build for CDP DC 7.0
### Build the hive module for QAT 
#### 1. Run the scripts    
    $ cd columnar_format_qat_wrapper
    $ ./apply_hive_jars.sh 7.0.0 $PATH/TO/IntelQATCodec

After this, we can see that in the folder under columnar_format_qat_wrapper/target, there have four parts: (1) parquet-format (2) parquet-mr (3) orc (4) hive
#### 2. Building the Parquet-format
1. go to the folder target/parquet-format
2. Please refer to documentation at
[building](https://github.com/apache/parquet-format/tree/apache-parquet-format-2.4.0#building)
for detailed prerequisites and guidance on building parquet-format.

#### 3. Building the Parquet-mr
1. Install Protobuf 3.5.1
2. Install Thrift 0.9.3
3. go to the folder target/parquet-mr
4. Please refer to the documentation at
   [building](https://github.com/apache/parquet-mr/tree/apache-parquet-1.10.0#building)
   for detailed prerequisites and guidance on building parquet-mr.

#### 4. Building the ORC
1. Install java 1.7 or higher
2. Install maven 3 or higher
3. Install cmake
4. go to the folder target/orc
5. Please refer to the documentation at [building](https://github.com/apache/orc/tree/rel/release-1.5.1#building) for detailed prerequisites and guidance on buidling orc.

#### 5. Building the Hive
1. go to the folder target/hive
2. Please refer to the documentation at [getting-started](https://github.com/apache/hive#getting-started) for detailed prerequisites and guidance on buidling hive.

#### 6. Copy the jars to the CDP
Please copy the following jars obtained in the previous steps to appropriate location in CDP.
```
parquet-format-2.4.0.jar
parquet-common-1.10.0.jar
parquet-hadoop-1.10.0.jar
orc-core-1.5.1.jar
orc-shims-1.5.1.jar
hive-exec-3.1.0.jar
```
## How to use QATCodec for Spark SQL Paruquet Datasource
### 1. Copy the jars to the Spark
Please copy the following jars obtained in the previous steps to appropriate location in Spark
```
parquet-format-2.4.0.jar
parquet-common-1.10.1.jar
parquet-hadoop-1.10.1.jar
```
### 2. Configuration to enable QATCodec
Put below configurations to _$SPARK_HOME/conf/spark-defaults.conf_ or via _spark-shell --conf_
```
spark.sql.parquet.compression.codec gzip
spark.hadoop.io.compression.codec.qat.enable true
```
#### For any security concerns, please visit https://01.org/security.
