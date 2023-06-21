$action  = New-ScheduledTaskAction -Execute '${smssender_batch}' -Argument '' -WorkingDirectory ''
$trigger = New-ScheduledTaskTrigger -Daily -At 1am
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "SMSSender Job" -Description "Daily SMSSender Job"
