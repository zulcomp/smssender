@ECHO OFF
echo Installing Java COMM API to JRE....
if "%OS%" == "Windows_NT" setlocal
CD ${INSTALL_PATH}\commapi

rem if "%PROCESSOR_ARCHITECTURE%" == "x86" goto copyx86
rem if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto copyx64

:copyx86

copy /Y win32com.dll "${jre_install}\bin"
copy /Y comm.jar "${jre_install}\lib\ext"
copy /Y javax.comm.properties "${jre_install}\lib"


goto end_line
:copyx64 
copy /Y win64com.dll "${jre_install}\bin"
copy /Y comm.jar "${jre_install}\lib\ext"
copy /Y javax.comm.properties "${jre_install}\lib"

:end_line
echo Finish Installing Java COMM API to JRE..
