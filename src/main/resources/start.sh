#!/usr/bin/env bash
#echo jar: $TARGET_JAR
echo pid: $TARGET_PID
echo startClass: $TARGET_CLASS
export LOCAL_RMI_PORT=$RMI_PORT
echo localRmiPort: $LOCAL_RMI_PORT
source ../environment.sh
echo Environment: $CONFIGUREME_ENVIRONMENT

#create classpath, first localconf, then service specific conf, then conf, after this add all jars in locallib, then lib.
#CLASSPATH="localconf:conf/$SERVICE_NAME:conf" <-- removed service specific conf for testing
CLASSPATH="localconf:conf"
for i in $( ls locallib/); do
	CLASSPATH="$CLASSPATH:locallib/$i"
done
echo CLASSPATH=$CLASSPATH
for i in $( ls lib/); do
	CLASSPATH="$CLASSPATH:lib/$i"
done

if [ "$JVM_OPTIONS" = "none" ]; then
    echo "no JVM Options set, using standard memory options "
    JVM_OPTIONS="-Xmx256M -Xms64M"
fi

export PROCESS_PROPERTIES="-Dpidfile=$TARGET_PID -Dconfigureme.defaultEnvironment=$CONFIGUREME_ENVIRONMENT $JVM_OPTIONS"
export PROCESS_PROPERTIES="$PROCESS_PROPERTIES -XX:+DisableExplicitGC -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:logs/gc.log"
##Java 8 GC Options.
export PROCESS_PROPERTIES="$PROCESS_PROPERTIES -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintReferenceGC"


if [[ ($LOCAL_RMI_PORT -eq "0") ]]; then
    echo "no port set, using random port"
else
    echo "setting to port $LOCAL_RMI_PORT"
	PROCESS_PROPERTIES="$PROCESS_PROPERTIES -DlocalRmiRegistryPort=$LOCAL_RMI_PORT"
fi

echo Properties: $PROCESS_PROPERTIES
nohup java -cp $CLASSPATH $PROCESS_PROPERTIES $TARGET_JAR >logs/stdout 2>logs/stderr $TARGET_CLASS &