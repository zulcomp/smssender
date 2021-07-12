Set wShell = CreateObject ("Wscript.Shell")
wShell.Run "SchTasks /Create /SC WEEKLY /D MON,TUE,WED,THU,FRI /TN ""Test Task"" /TR ""C:\test.bat"" /ST 16:30", 0