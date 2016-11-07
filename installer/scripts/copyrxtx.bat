@ECHO OFF
echo Installing RXTX API to JRE....
if "%OS%" == "Windows_NT" setlocal
CD ${INSTALL_PATH}\rxtx

rem if "%PROCESSOR_ARCHITECTURE%" == "x86" goto copyx86
rem if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto copyx64

:copyx86

copy /Y rxtxSerial.dll "${jre_install}\bin"
copy /Y rxtxParallel.dll "${jre_install}\bin"
rem copy /Y comm.jar "${jre_install}\lib\ext"
rem copy /Y javax.comm.properties "${jre_install}\lib"


goto end_line
:copyx64 
rem copy /Y win64com.dll "${jre_install}\bin"
rem copy /Y comm.jar "${jre_install}\lib\ext"
rem copy /Y javax.comm.properties "${jre_install}\lib"

:end_line
echo Finish Installing RXTX API to JRE..
EXIT /B 0