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

declare -a supported_Lucene_versions=("8.2.0")
#declare -A es_lucene_version_m=(["8.0.0"]="8.2.0")


# Repo Address
LUCENE_REPO=https://github.com/apache/lucene-solr.git
ES_version_base="8.0.0"
Lucene_version_base="8.2.0"
Lucene_baseCommitID=e8494222be8a44678b43f907b47accbda3ebd401


LUCENE_version=$1
QATCodec_SRC_DIR=$2

LUCENE_QAT_DIR=$QATCodec_SRC_DIR/es_qat_wrapper/${ES_version_base}/lucene-${Lucene_version_base}/lucene
TARGET_DIR=$QATCodec_SRC_DIR/es_qat_wrapper/${ES_version_base}/target
LUCENE_SRC_DIR=$TARGET_DIR/LUCENE


function clone_repo(){
  echo "Clone from Repo $1"
  git clone $1 $LUCENE_SRC_DIR
}

function checkout_branch(){
  pushd $LUCENE_SRC_DIR
  Branch_name=VERSION-${Lucene_version_base}
  git checkout -b $Branch_name $Lucene_baseCommitID
  popd
}

function usage(){
  printf "Usage: sh build_lucene_jars.sh lucene_version [PATH/TO/QAT_Codec_SRC]\n (e.g. sh build_lucene_jars.sh 8.2.0 /home/user/workspace/QATCodec)\n"
  exit 1
}

function check_LUCENE_version(){
  valid_version=false
  for v in $supported_Lucene_versions
  do
  	if [ "$v" =  "$1" ]; then
		valid_version=true
    		break;
	fi
  done
  if ! $valid_version ; then
  	printf "Unsupported Lucene version $1, current supported versions include: ${supported_Lucene_versions[@]} \n"
  	exit 1
  fi
}

apply_patch_to_lucene(){
  pushd $TARGET_DIR
  clone_repo $LUCENE_REPO
  checkout_branch
  echo yes | cp -rf $LUCENE_QAT_DIR $LUCENE_SRC_DIR/
  popd
}
if [ "$#" -ne 2 ]; then
  usage
fi


check_LUCENE_version $LUCENE_version

if [ -d $TARGET_DIR ]; then
  echo "$TARGET_DIR is not clean"
else
  mkdir -p $TARGET_DIR
fi

apply_patch_to_lucene
