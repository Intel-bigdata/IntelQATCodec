AT Codec installation in non-cdh/manually installed cluster
--------------------------------------------------------------

1. Copy the jar file and .so file in the appropriate location of standalone installation
        Step 1: Copy the hadoop_qat_codec*.jar file to $HADOOP_COMMON_HOME/ share/hadoop/common
        Step 2: Copy the libqatcodec.so file to $HADOOP_COMMON_HOME/lib/native
        Step 3 (optional, required if use Hive): Create soft link for Parquet related jars (parquet-format-[CDH_Version][QAT codec version].jar and parquet-hadoop-bundle-[CDH_version][QAT codec version]) and Hive related jars (hive-exec-[CDH_Version][QAT codec version].jar, hive-shim-0.23-[CDH_version][QAT codec version].jar and hive-shims-common-[CDH_version][QAT codec version].jar) to CDH jars folder ([path/to/CDH/lib/hive/]).

2. Configuring in mapred-site.xml
        Step 1: Copy the hadoop_qat_codec*.jar and libqatcodec.so file to the same location in all the nodes in the cluster
        Step 2: Add the location of hadoop_qat_codec*.jar to mapreduce.application.classpath in mapred-site.xml or yarn.application.classpath in yarn-site.xml.
        Step 3: Add the location of libqatcodec.so file to mapreduce.admin.user.env in mapred-site.xml.

3. Configuring in spark-defaults.conf
   Step 1: Copy the spark_qat_codec*.jar to the same location in all the nodes in the cluster
   Step 2: Add the location of spark_qat_codec*.jar to spark.driver.extraClassPath and spark.executor.extraClassPath in spark-defaults.conf
