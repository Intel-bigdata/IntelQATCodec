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
#!/bin/bash

declare -a supported_CDH_versions=("5.14.2")
declare -A cdh_parquet_format_version_m=( ["5.14.2"]="2.1.0")
declare -A cdh_parquet_mr_version_m=( ["5.14.2"]="1.5.0")
declare -A cdh_hive_version_m=( ["5.14.2"]="1.1.0")
# Repo Address
PARQUET_MR_REPO=https://github.com/cloudera/parquet-mr
PARQUET_FORMAT_REPO=https://github.com/cloudera/parquet-format
HIVE_REPO=https://github.com/cloudera/hive

function clone_repo(){
  echo "Clone Branch $1 from Repo $2"
  git clone -b $1 --single-branch $2
}

function usage(){
  printf "Usage: sh build_hive_jars.sh CDH_release_version [PATH/TO/QAT_Codec_SRC]\n (e.g. sh build_hive_jars.sh 5.14.2 /home/user/workspace/QATCodec)\n"
  exit 1
}

function check_CDH_version(){
  valid_version=false
  for v in $supported_CDH_versions
  do
  	if [ "$v" =  "$1" ]; then
		valid_version=true
    		break;
	fi
  done
  if ! $valid_version ; then
  	printf "Unsupported CDH version $1, current supported versions include: ${supported_CDH_versions[@]} \n"
  	exit 1
  fi
}

apply_patch_to_cdh_hive(){
  pushd $TARGET_DIR
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  HIVE_BRANCH="cdh$CDH_major_version-${cdh_hive_version_m[$CDH_release_version]}_$CDH_release_version"
  clone_repo $HIVE_BRANCH $HIVE_REPO
  echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/hive $TARGET_DIR/
  popd
}

apply_patch_to_cdh_parquet_format(){
  pushd $TARGET_DIR
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  PARQUET_FORMAT_BRANCH="cdh$CDH_major_version-${cdh_parquet_format_version_m[$CDH_release_version]}_$CDH_release_version"
  clone_repo $PARQUET_FORMAT_BRANCH $PARQUET_FORMAT_REPO
  echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/parquet-format $TARGET_DIR/
  popd
}

apply_patch_to_cdh_parquet_mr(){
  pushd $TARGET_DIR
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  PARQUET_MR_BRANCH="cdh$CDH_major_version-${cdh_parquet_mr_version_m[$CDH_release_version]}_$CDH_release_version"
  clone_repo $PARQUET_MR_BRANCH $PARQUET_MR_REPO
  echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/parquet-mr $TARGET_DIR/
  popd
}

if [ "$#" -ne 2 ]; then
  usage
fi

CDH_release_version=$1
check_CDH_version $CDH_release_version

QATCodec_SRC_DIR=$2
HIVE_QAT_DIR=$QATCodec_SRC_DIR/columnar_format_qat_wrapper
TARGET_DIR=$HIVE_QAT_DIR/target

if [ -d $TARGET_DIR ]; then
  echo "$TARGET_DIR is not clean"
else
  mkdir -p $TARGET_DIR
fi

apply_patch_to_cdh_parquet_format
apply_patch_to_cdh_parquet_mr
apply_patch_to_cdh_hive
