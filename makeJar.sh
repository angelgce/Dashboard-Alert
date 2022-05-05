#!/usr/bin/bash

rm -rf MANIFEST.MF DBAlert.jar

javac -cp .:./lib/sqlite-jdbc-3.36.0.3.jar:./lib/json-path-2.6.0.jar:./lib/json-smart-2.1.0.jar:./lib/asm-1.0.2.jar:./lib/slf4j-api-2.0.0-alpha5.jar:./lib/j-text-utils-0.3.4.jar:./lib/httpclient-4.5.10.jar:./lib/httpcore-4.4.14.jar:./lib/commons-cli-1.4.jar:./lib/commons-logging-1.2.jar:./lib/postgresql-42.3.1.jar:./lib/ini4j-0.5.4.jar:./lib/spark-core-2.6.0.jar:./lib/junit-4.10.jar:./lib/javax.servlet-api-3.1.0.jar:./lib/jetty-server-9.4.4.v20170414.jar:./lib/jetty-util-9.4.4.v20170414.jar:./lib/jetty-http-9.4.4.v20170414.jar:./lib/jetty-io-9.4.4.v20170414.jar ./*.java

echo Main-Class: API > MANIFEST.MF

jar -cvmf MANIFEST.MF FinderAPI.jar *.class

java -cp .:./lib/sqlite-jdbc-3.36.0.3.jar:./lib/json-path-2.6.0.jar:./lib/json-smart-2.1.0.jar:./lib/asm-1.0.2.jar:./lib/slf4j-api-2.0.0-alpha5.jar:./lib/j-text-utils-0.3.4.jar:./lib/httpclient-4.5.10.jar:./lib/httpcore-4.4.14.jar:./lib/commons-cli-1.4.jar:./lib/commons-logging-1.2.jar:./lib/postgresql-42.3.1.jar:./lib/ini4j-0.5.4.jar:./lib/spark-core-2.6.0.jar:./lib/junit-4.10.jar:./lib/javax.servlet-api-3.1.0.jar:./lib/jetty-server-9.4.4.v20170414.jar:./lib/jetty-util-9.4.4.v20170414.jar:./lib/jetty-http-9.4.4.v20170414.jar:./lib/jetty-io-9.4.4.v20170414.jar API -c ./config.ini
