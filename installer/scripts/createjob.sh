#!/bin/sh
cd ${INSTALL_PATH}/scripts
crontab -l > tempcron
cat tempcron createjob.txt > tempcron1
crontab tempcron1
rm tempcron
rm tempcron1
