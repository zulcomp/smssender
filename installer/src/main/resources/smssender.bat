@echo off
if "%OS%" == "Windows_NT" setlocal
CD ${INSTALL_PATH}
SET PATH=${jre_install}\bin;%PATH%;
IF "%1"=="" goto without_cmdline
java -jar "smssender.jar" %*
goto end_line
:without_cmdline
java -jar "smssender.jar"
:end_line
exit 0
