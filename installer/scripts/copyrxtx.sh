#!/bin/sh

cd ${INSTALL_PATH}/rxtx
echo "Installing RXTX API to JRE...."

cp -f -u librxtxSerial.so ${jre_install}/bin
if [ -f librxtxParallel.so ]; 
then
   cp -f -u librxtxParallel.so ${jre_install}/bin
fi
#cp -f -u RXTXcomm.jar ${jre_install}/lib/ext
#cp -f -u javax.comm.properties ${jre_install}/lib

echo "Finish Installing RXTX API to JRE.."
exit 0

