@echo off
if "%OS%" == "Windows_NT" setlocal
CD ${INSTALL_PATH}
SET PATH=${jre_install}\bin;%PATH%;
IF "%1"=="" goto without_cmdline
java -jar "smssender.jar" %1 %2 %3 %4 %5
goto end_line
:without_cmdline
java -jar "smssender.jar"
:end_line
exit 0
