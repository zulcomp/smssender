Set wShell = CreateObject ("Wscript.Shell")
wShell.Run "SchTasks /Create /SC WEEKLY /D MON,TUE,WED,THU,FRI /TN ""SMSSender Task"" /TR ""${smssender_batch}"" /ST 16:30", 0