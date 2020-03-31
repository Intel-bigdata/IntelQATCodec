#How to deploy the Elasticsearch with QAT
  
##I. Install the QATzip and QAT Drivers

1. Update 
       
       $ sudo yum update
   
2. Create new file intel-qat.repo  in  /etc/yum.repos.d, the content is as follows: 
    
        [intel-qat]
        name=Intel QAT
        baseurl=https://download.01.org/QAT/repo
        gpgcheck=0
3. Install
   
        $ sudo yum install QAT
        $ sudo yum install QATzip
        
4. Configure huge page related setting

       $ sudo cat  /etc/sysctl.conf    
      ensure the line vm.nr_hugepages=512 exists in the file  	      
       
       $ sudo sysctl -p
       $ sudo cat /proc/meminfo | grep -i hugepages_total
                 HugePages_Total: 512 
  
5. Config QAT Codec
     
       $ chmod 777 $ICP_ROOT/build
       $ chmod 777 $ICP_ROOT/build/libusdm_drv_s.so 
       $ chmod 777 /dev/qat_adf_ctl
       $ chmod 777 /dev/qat_dev_processes
       $ chmod 777 /dev/uio*
       $ chmod 777 /dev/usdm_drv
       $ chmod -R 777 /dev/hugepages
 
     The $ICP_ROOT/  is in  /opt/intel/QAT by default, and the $DZ_ROOT is in
/opt/intel/QATzip  by default
6. Start the qat service 
      
       $ sudo service qat_service start 
7. Run the following command to check if the QATzip is set up correctly for compressing or decompressing files.
      
       $ qzip -k $your_input_file
##II. Copy the .so files to the /lib64   
       $ sudo cp libQatCodecEs.so /lib64
       $ sudo cp libqatcodec.so /lib64 
       
##III. Unzip the Elasticsearch binary file
        $ tar -zcvf elasticsearch-7.6.1-SNAPSHOT-linux-x86_64.tar.gz
##IV. Start the Elaticsearch service
1. Config the elasticsearch settings
       
       $ vim config/elasticsearch.yml
2. Start the Elasticsearch service
      
       $ bin/elasticsearch

#How to build 
## I. Set the environments
1. Install JDK

we need jdk13 for Elasticsearch 7.6.1 and jdk8 for IntelQatCodec
        
        1. $ sudo yum install -y curl
        2. $ curl -O https://download.java.net/java/GA/jdk13/5b8a42f3905b406298b72d750b6919f6/33/GPL/openjdk-13_linux-x64_bin.tar.gz
        3. $ tar xvf openjdk-13_linux-x64_bin.tar.gz
        4. $ sudo mv jdk-13 /opt/
        5. Configure the java environment
	         export JAVA_HOME=/root/jdk-13
             export PATH=$JAVA_HOME/bin:$PATH
        6. Confirm the java version
   	         $ java -version

2.Install Gradle
          
    1. Download Gradle
         $ wget https://services.gradle.org/distributions/gradle-5.2.1-bin.zip -P /tmp
    2. Install Gradle
         $ sudo unzip -d /opt/gradle /tmp/gradle-*.zip
    3. Setup environment variables
         export GRADLE_HOME=/opt/gradle/gradle-5.2.1
         export PATH=${GRADLE_HOME}/bin:${PATH}
    4. Check if the Gradle install was successful.
         $gradle -v
3.Install maven
     
    1. $ sudo yum install maven
    2. $ mvn -version
 
## II. Build IntelQATCodec
    
    $ cd $IntelQATCodecSrcDri
    $ mvn clean install -Dqatzip.libs=/opt/intel/QATzip -Dqatzip.src=/opt/intel/QATzip -DskipTests
 Then we can get the following files that we need.
     
      $IntelQATCodecSrcDri/lucene_qat_wrapper/target/lucene_qat_wrapper.jar 
      $IntelQATCodecSrcDri/lucene_qat_wrapper/target/libqatcodec.so
      $IntelQATCodecSrcDri/es_qat_wrapper/target/es_qat_wrapper.jar
      $IntelQATCodecSrcDri/es_qat_wrapper/target/classes/com/intel/qat/native/lib/Linux/amd64/libQatCodecEs.so
 
 We need to copy these files to other places:
      
      $ sudo cp libqatcodec.so libQatCodecEs.so /lib64
      $ cp lucene_qat_wrapper.jar es_qat_wrapper/7.6.1/lucene-8.4.0/lucene/lib/
      $ cp es_qat_wrapper.jar es_qat_wrapper/7.6.1/elasticsearch/buildSrc/libs/

## III. Apply the lucene patch     

      $ cd $IntelQATCodecSrcDri/elasticsearch_qat_wrapper/7.6.1
      $ ./apply_lucene_jars.sh 8.4.0 $IntelQATCodecSrcDri

##IV. Build the lucene in target folder

    $ cd $IntelQATCodecSrcDri/elasticsearch_qat_wrapper/7.6.1/target/LUCENE
    $ ant -autoproxy clean compile 
Then we need to copy the jars to the ./elasticsearch_qat_wrapper/7.6.1/elasticsearch/buildSrc/libs/
 
     lucene-core-8.4.0-SNAPSHOT.jar
     lucene_qat_wrapper.jar
##V. Apply the elastcsearch patch
      $ cd $IntelQATCodecSrcDri/elasticsearch_qat_wrapper/7.6.1
      $ ./apply_es_jars.sh 7.6.1 $IntelQATCodecSrcDri

##VI. Build the  elasticsearch in target folder
      
      $ java -version
To make sure the java version is 11+.(We use jdk13)

      $ cd $IntelQATCodecSrcDri/elasticsearch_qat_wrapper/7.6.1/target/elasticsearch
      $ ./gradlew :distribution:archives:linux-tar:assemble --parallel
and then we can get the binary files in the ./distribution/archives/linux-tar/build/distributions/
