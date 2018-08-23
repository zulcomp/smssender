#!/bin/sh
cd ${INSTALL_PATH}

if [ $# -eq 0 ]; then
    ${jre_install}/bin/java -jar "smssender.jar"
else
    ${jre_install}/bin/java -jar "smssender.jar" $*
fi
exit 0