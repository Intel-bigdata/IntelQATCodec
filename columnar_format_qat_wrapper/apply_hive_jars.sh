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

declare -a supported_CDH_versions=("5.14.2" "6.2.1" "7.0.0" "spark3.0.0")
declare -A cdh_parquet_format_version_m=( ["5.14.2"]="2.1.0" ["7.0.0"]="2.4.0" ["spark3.0.0"]="2.4.0")
declare -A cdh_parquet_mr_version_m=( ["5.14.2"]="1.5.0" ["7.0.0"]="1.10.0" ["spark3.0.0"]="1.10.1")
declare -A cdp_orc_version_m=(["7.0.0"]="1.5.1" ["spark3.0.0"]="1.5.10")
declare -A cdh_hive_version_m=( ["5.14.2"]="1.1.0" ["7.0.0"]="3.1.0"  ["spark3.0.0"]="2.3.7")
# Repo Address
PARQUET_MR_REPO=https://github.com/cloudera/parquet-mr
PARQUET_FORMAT_REPO=https://github.com/cloudera/parquet-format
HIVE_REPO=https://github.com/cloudera/hive

##upstream Repo Address
UPSTREAM_PARQUET_MR_REPO=https://github.com/apache/parquet-mr.git
UPSTREAM_PARQUET_FORMAT_REPO=https://github.com/apache/parquet-format.git
UPSTREAM_ORC_REPO=https://github.com/apache/orc.git
UPSTREAM_HIVE_REPO=https://github.com/apache/hive.git

CDH_release_version=$1
QATCodec_SRC_DIR=$2
HIVE_QAT_DIR=$QATCodec_SRC_DIR/columnar_format_qat_wrapper
TARGET_DIR=$HIVE_QAT_DIR/target

ORC_SRC_DIR=${TARGET_DIR}/orc
HIVE_SRC_DIR=${TARGET_DIR}/hive
PARQUET_MR_SRC_DIR=${TARGET_DIR}/parquet-mr
PARQUET_FORMAT_SRC_DIR=${TARGET_DIR}/parquet-format

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
  for v in ${supported_CDH_versions[@]}
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

apply_patch_to_cdp_orc(){
   CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
   if [ "$CDH_major_version" != "spark3" ]; then
       pushd $TARGET_DIR
       if [ "$CDH_major_version" = "7" ]; then
          ORC_BRANCH="rel/release-${cdp_orc_version_m[$CDH_release_version]}"
       fi
       clone_repo $ORC_BRANCH $UPSTREAM_ORC_REPO
       popd
       apply_diff_to_orc
   fi
}

apply_diff_to_orc(){
   CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
   if [ "$CDH_major_version" = "7" ]; then
      pushd $ORC_SRC_DIR
      git apply --reject --whitespace=fix $HIVE_QAT_DIR/$CDH_release_version/orc/*.diff
      popd
   fi
}

apply_patch_to_cdh_hive(){
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  if [ "$CDH_major_version" != "spark3" ]; then
      pushd $TARGET_DIR
      if [ "$CDH_major_version" = "6" ]; then
        HIVE_BRANCH="cdh$CDH_release_version"
      elif [ "$CDH_major_version" = "7" ]; then
        HIVE_BRANCH="rel/release-${cdh_hive_version_m[$CDH_release_version]}"
        clone_repo $HIVE_BRANCH $UPSTREAM_HIVE_REPO
        popd
        apply_diff_to_hive
        return
      else
        HIVE_BRANCH="cdh$CDH_major_version-${cdh_hive_version_m[$CDH_release_version]}_$CDH_release_version"
      fi
      clone_repo $HIVE_BRANCH $HIVE_REPO
      echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/hive $TARGET_DIR/
      popd
  fi
}

apply_diff_to_hive(){
   CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
   if [ "$CDH_major_version" = "7" ]; then
      pushd $HIVE_SRC_DIR
      git apply --reject --whitespace=fix $HIVE_QAT_DIR/$CDH_release_version/hive/*.diff
      popd
   fi
}

apply_patch_to_cdh_parquet_format(){
  pushd $TARGET_DIR
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  if [ "$CDH_major_version" = "6" ]; then
    PARQUET_FORMAT_BRANCH="cdh$CDH_release_version"
  elif [ "$CDH_major_version" = "7" ] || [ "$CDH_major_version" = "spark3" ]; then
    PARQUET_FORMAT_BRANCH="apache-parquet-format-${cdh_parquet_format_version_m[$CDH_release_version]}"
    clone_repo $PARQUET_FORMAT_BRANCH $UPSTREAM_PARQUET_FORMAT_REPO
    popd
    apply_diff_to_parquet_format
    return
  else
    PARQUET_FORMAT_BRANCH="cdh$CDH_major_version-${cdh_parquet_format_version_m[$CDH_release_version]}_$CDH_release_version"
  fi
  clone_repo $PARQUET_FORMAT_BRANCH $PARQUET_FORMAT_REPO
  echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/parquet-format $TARGET_DIR/
  popd
}

apply_diff_to_parquet_format(){
   CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
   if [ "$CDH_major_version" = "7" ] || [ "$CDH_major_version" = "spark3" ]; then
      pushd $PARQUET_FORMAT_SRC_DIR
      git apply --reject --whitespace=fix $HIVE_QAT_DIR/$CDH_release_version/parquet-format/*.diff
      popd
   fi
}

apply_patch_to_cdh_parquet_mr(){
  pushd $TARGET_DIR
  CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
  if [ "$CDH_major_version" = "6" ]; then
    PARQUET_MR_BRANCH="cdh$CDH_release_version"
  elif [ "$CDH_major_version" = "7" ] || [ "$CDH_major_version" = "spark3" ]; then
    PARQUET_MR_BRANCH="apache-parquet-${cdh_parquet_mr_version_m[$CDH_release_version]}"
    clone_repo $PARQUET_MR_BRANCH $UPSTREAM_PARQUET_MR_REPO
    popd
    apply_diff_to_parquet_mr
    return
  else
    PARQUET_MR_BRANCH="cdh$CDH_major_version-${cdh_parquet_mr_version_m[$CDH_release_version]}_$CDH_release_version"
  fi
  clone_repo $PARQUET_MR_BRANCH $PARQUET_MR_REPO
  echo yes | cp -rf $HIVE_QAT_DIR/$CDH_release_version/parquet-mr $TARGET_DIR/
  popd
}

apply_diff_to_parquet_mr(){
   CDH_major_version=$(echo $CDH_release_version | cut -d '.' -f 1)
   if [ "$CDH_major_version" = "7" ] || [ "$CDH_major_version" = "spark3" ]; then
      pushd $PARQUET_MR_SRC_DIR
      git apply --reject --whitespace=fix $HIVE_QAT_DIR/$CDH_release_version/parquet-mr/*.diff
      popd
   fi
}

if [ "$#" -ne 2 ]; then
  usage
fi

check_CDH_version $CDH_release_version

if [ -d $TARGET_DIR ]; then
  echo "$TARGET_DIR is not clean"
else
  mkdir -p $TARGET_DIR
fi

apply_patch_to_cdh_parquet_format
apply_patch_to_cdh_parquet_mr
apply_patch_to_cdp_orc
apply_patch_to_cdh_hive
