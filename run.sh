#/usr/bin/bash

CP=/home/Abricot/ServerAlert-API/models
LIB=/home/Abricot/ServerAlert-API/libraries

clear
clear


javac -cp $CP:$LIB/sqlite-jdbc-3.36.0.3.jar:$LIB/json-path-2.6.0.jar:$LIB/json-smart-2.1.0.jar:$LIB/asm-1.0.2.jar:$LIB/slf4j-api-2.0.0-alpha5.jar:$LIB/j-text-utils-0.3.4.jar:$LIB/httpclient-4.5.10.jar:$LIB/httpcore-4.4.14.jar:$LIB/commons-cli-1.4.jar:$LIB/commons-logging-1.2.jar:$LIB/postgresql-42.3.1.jar:$LIB/ini4j-0.5.4.jar:$LIB/spark-core-2.6.0.jar:$LIB/junit-4.10.jar:$LIB/javax.servlet-api-3.1.0.jar:$LIB/jetty-server-9.4.4.v20170414.jar:$LIB/jetty-util-9.4.4.v20170414.jar:$LIB/jetty-http-9.4.4.v20170414.jar:$LIB/jetty-io-9.4.4.v20170414.jar:$LIB/log4j-core-2.17.2.jar $CP/*.java

java -cp $CP:$LIB/sqlite-jdbc-3.36.0.3.jar:$LIB/json-path-2.6.0.jar:$LIB/json-smart-2.1.0.jar:$LIB/asm-1.0.2.jar:$LIB/slf4j-api-2.0.0-alpha5.jar:$LIB/j-text-utils-0.3.4.jar:$LIB/httpclient-4.5.10.jar:$LIB/httpcore-4.4.14.jar:$LIB/commons-cli-1.4.jar:$LIB/commons-logging-1.2.jar:$LIB/postgresql-42.3.1.jar:$LIB/ini4j-0.5.4.jar:$LIB/spark-core-2.6.0.jar:$LIB/junit-4.10.jar:$LIB/javax.servlet-api-3.1.0.jar:$LIB/jetty-server-9.4.4.v20170414.jar:$LIB/jetty-util-9.4.4.v20170414.jar:$LIB/jetty-http-9.4.4.v20170414.jar:$LIB/jetty-io-9.4.4.v20170414.jar:$LIB/log4j-core-2.17.2.jar API -c ./config.ini
