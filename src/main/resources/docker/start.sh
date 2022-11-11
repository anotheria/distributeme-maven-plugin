#!/bin/ash

CLASSPATH="conf"
echo CLASSPATH=$CLASSPATH
for i in $( ls lib/); do
	CLASSPATH="$CLASSPATH:lib/$i"
done

if [ "$JVM_OPTIONS" = "none" ]; then
    echo "no JVM Options set, using standard memory options "
    JVM_OPTIONS="-Xmx256M -Xms64M"
fi

echo CLASSPATH: $CLASSPATH
echo "ServiceClass (SERVICE_CLASS): $SERVICE_CLASS"
echo "Service Registration IP: (SERVICE_REGISTRATION_IP): $SERVICE_REGISTRATION_IP"
echo "ServicePort (SERVICE_PORT): $SERVICE_PORT"
echo "Starting service $SERVICE_CLASS running at $SERVICE_REGISTRATION_IP:$SERVICE_PORT"
OPTIONS="-DserviceBindingPort=$SERVICE_PORT -DlocalRmiRegistryPort=$SERVICE_PORT "
OPTIONS="$OPTIONS -Dcom.sun.management.jmxremote.host=$SERVICE_REGISTRATION_IP -Djava.rmi.server.logCalls=true -Djava.rmi.server.hostname=$SERVICE_REGISTRATION_IP"
OPTIONS="$OPTIONS -DregistrationHostName=$SERVICE_REGISTRATION_IP"
echo Options: $OPTIONS
java -Xmx256M -Xms64M $OPTIONS -classpath $CLASSPATH -Dconfigureme.defaultEnvironment=test $SERVICE_CLASS
