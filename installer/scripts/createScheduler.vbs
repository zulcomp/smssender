strComputer = "."
Set objWMIService = GetObject("winmgmts:" _
    & "{impersonationLevel=impersonate}!\\" & strComputer & "\root\cimv2")
Set objNewJob = objWMIService.Get("Win32_ScheduledJob")
errJobCreated = objNewJob.Create _
    ("${INSTALL_PATH}\SmsSender.bat", "********230000.000000-420", _
        True ,1 OR 2 OR 4 OR 8 OR 16 OR 32 OR 64, , , JobID)
Wscript.Echo errJobCreated