#!/usr/bin/env bash
#echo localRmiPort: $LOCAL_RMI_PORT
#echo jar: $TARGET_JAR
echo pid: $TARGET_PID
echo startClass: $TARGET_CLASS
source ../environment.sh
echo Environment: $CONFIGUREME_ENVIRONMENT

#create classpath
CLASSPATH="conf"
for i in $( ls locallib/); do
	CLASSPATH="$CLASSPATH:locallib/$i"
done
echo CLASSPATH=$CLASSPATH
for i in $( ls lib/); do
	CLASSPATH="$CLASSPATH:lib/$i"
done

#echo CLASSPATH=$CLASSPATH
#nohup java -Xmx256M -Xms64M -jar -DlocalRmiRegistryPort=$LOCAL_RMI_PORT -Dloader.path="config/" -Dconfigureme.defaultEnvironment=$CONFIGUREME_ENVIRONMENT $TARGET_JAR >logs/stdout 2>logs/stderr & echo $! > $TARGET_PID
nohup java -Xmx256M -Xms64M -cp $CLASSPATH -Dpidfile=$TARGET_PID -Dconfigureme.defaultEnvironment=$CONFIGUREME_ENVIRONMENT $TARGET_JAR >logs/stdout 2>logs/stderr $TARGET_CLASS &