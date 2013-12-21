#!/bin/bash
# Path to weka
WEKA_PATH=/home/shu/git/Research/jars
# add mysql-connector (manually copied to weka path) and weka to classpath
CP="$CLASSPATH:/usr/share/java/:$WEKA_PATH/mysql-connector-java-5.1.27-bin.jar:$WEKA_PATH/weka.jar"
# use the connector of debian package libmysql-java
# CP="$CLASSPATH:/usr/share/java/:$WEKA_PATH/weka.jar"
echo "used CLASSPATH: $CP"
# start Explorer
java -cp $CP -Xmx500m weka.gui.explorer.Explorer
