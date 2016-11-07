#!/bin/sh

cd ${INSTALL_PATH}
echo "Installing Java COMM API to JRE...."

cp -f -u libLinuxSerialParallel.so ${jre_install}/bin
cp -f -u comm.jar ${jre_install}/lib/ext
cp -f -u javax.comm.properties ${jre_install}/lib

echo "Finish Installing Java COMM API to JRE.."
exit 0

