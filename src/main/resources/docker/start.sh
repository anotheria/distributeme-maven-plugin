#!/bin/bash

CLASSPATH="conf"
echo CLASSPATH=$CLASSPATH
for i in $( ls lib/); do
	CLASSPATH="$CLASSPATH:lib/$i"
done

if [ "$JVM_OPTIONS" = "null" ]; then
    echo "no JVM Options set, using standard memory options "
    JVM_OPTIONS="-Xmx256M -Xms64M"
fi

if [ "$GOOGLE_APPLICATION_CREDENTIALS_FILE" != "null" ]; then
    export GOOGLE_APPLICATION_CREDENTIALS=/app/conf/$GOOGLE_APPLICATION_CREDENTIALS_FILE
fi

echo CLASSPATH: $CLASSPATH
echo "ServiceClass (SERVICE_CLASS): $SERVICE_CLASS"
echo "Service Registration IP: (SERVICE_REGISTRATION_IP): $SERVICE_REGISTRATION_IP"
echo "ServicePort (SERVICE_PORT): $SERVICE_PORT"
echo "Starting service $SERVICE_CLASS running at $SERVICE_REGISTRATION_IP:$SERVICE_PORT"
echo "ConfigureMe environment: $CONFIGUREME_ENVIRONMENT"
OPTIONS="-DserviceBindingPort=$SERVICE_PORT -DlocalRmiRegistryPort=$SERVICE_PORT "
OPTIONS="$OPTIONS -Dcom.sun.management.jmxremote.host=$SERVICE_REGISTRATION_IP -Djava.rmi.server.logCalls=true -Djava.rmi.server.hostname=$SERVICE_REGISTRATION_IP"
OPTIONS="$OPTIONS -DregistrationHostName=$SERVICE_REGISTRATION_IP"
#add gc logging
OPTIONS="$OPTIONS -XX:+DisableExplicitGC -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:logs/gc.log"
##Java 8 GC Options.
OPTIONS="$OPTIONS -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintReferenceGC"


echo Options: $OPTIONS
echo Command: java $JVM_OPTIONS $OPTIONS -classpath $CLASSPATH -Dconfigureme.defaultEnvironment=$CONFIGUREME_ENVIRONMENT $SERVICE_CLASS
java $JVM_OPTIONS $OPTIONS -classpath $CLASSPATH -Dconfigureme.defaultEnvironment=$CONFIGUREME_ENVIRONMENT >logs/stdout 2>logs/stderr $SERVICE_CLASS
