@ECHO OFF
echo ...Configuring SMSSender
if "%OS%" == "Windows_NT" setlocal
CD ${INSTALL_PATH}
smssender.bat config %1
EXIT /B 0