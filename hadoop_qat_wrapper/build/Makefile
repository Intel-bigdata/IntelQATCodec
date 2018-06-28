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

qatcodec.jar: bin
	$(call check,JAVA_HOME)
	$(JAVA_HOME)/bin/jar cf qatcodec.jar -C bin org

bin: 
	# The codec depends on several Hadoop JARs. We need the value of
	# HADOOPJARS to know where these are located. If you have a
	# parcel-based Cloudera cluster, the default location is
	# /opt/cloudera/parcels/CDH/jars.
	$(call check,JAVA_HOME HADOOPJARS)
	mkdir bin
	$(JAVA_HOME)/bin/javac -d bin -cp "$(HADOOPJARS)/*" @filelist

clean:
	rm -fr qatcodec.jar bin
