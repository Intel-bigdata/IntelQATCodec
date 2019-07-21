#!/bin/bash
QATCODEC_VERSION=${parcel.version}
QATCODEC_DIRNAME=${PARCEL_DIRNAME:-"QATCODEC-${QATCODEC_VERSION}"}

if [ -n "${HADOOP_CLASSPATH}" ]; then
  export HADOOP_CLASSPATH="${HADOOP_CLASSPATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
else
  export HADOOP_CLASSPATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
fi

if [ -n "${MR2_CLASSPATH}" ]; then
  export MR2_CLASSPATH="${MR2_CLASSPATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
else
  export MR2_CLASSPATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
fi

if [ -n "${JAVA_LIBRARY_PATH}" ]; then
  export JAVA_LIBRARY_PATH="${JAVA_LIBRARY_PATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
else
  export JAVA_LIBRARY_PATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
fi

if [ -n "${LD_LIBRARY_PATH}" ]; then
  export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
else
  export LD_LIBRARY_PATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
fi

if [ -n "${CDH_SPARK_CLASSPATH}" ]; then
  export CDH_SPARK_CLASSPATH="${CDH_SPARK_CLASSPATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/spark/lib/*:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
else
  export CDH_SPARK_CLASSPATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/spark/lib/*:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/*"
fi

if [ -n "${SPARK_LIBRARY_PATH}" ]; then
  export SPARK_LIBRARY_PATH="${SPARK_LIBRARY_PATH}:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
else
  export SPARK_LIBRARY_PATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hadoop/lib/native"
fi

if [ -n "${AUX_CLASSPATH}" ]; then
  export AUX_CLASSPATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/parquet/lib/*:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hive/lib/*:${AUX_CLASSPATH}"
else
  export AUX_CLASSPATH="$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/parquet/lib/*:$PARCELS_ROOT/$QATCODEC_DIRNAME/lib/hive/lib/*"
fi

