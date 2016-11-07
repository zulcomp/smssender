@echo off
if "%OS%"=="Windows_NT" setlocal

CD ${INSTALL_PATH}\scripts

rem if "%PROCESSOR_ARCHITECTURE%" == "x86" goto createx86
rem if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto createx64

:createx86
rem C:\Windows\System32\CScript.exe //Nologo //B createScheduler.vbs

goto endline
:createx64
rem C:\Windows\System32\CScript.exe //Nologo //B createScheduler.vbs

:endline
schtasks /create /tn "SMSSender Jobs" /tr "${INSTALL_PATH}\smssender.bat" /sc DAILY /st 23:00:00 /ru SYSTEM
EXIT /B 0