# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-ProcessInfo {
  $ErrorActionPreference = "Continue" 
  $Processes = Get-WmiObject -query "Select * from Win32_Process"
  foreach ($Process in $Processes) {
    "[{0:D}]" -f $Process.ProcessId
    "PPID={0:D}" -f $Process.ParentProcessId
    "Priority={0:D}" -f $Process.Priority
    "CommandLine={0}" -f $Process.CommandLine

    # path is the current_dir + image_path
    if ($Process.CommandLine -ne $null) {
      "Path={0}" -f $Process.Path
    }
    if ($Process.CreationDate -ne $null) {
      "CreationDate={0}" -f $Process.CreationDate
    }
    try {
      $dep = [jOVAL.Process.Probe]::IsDepEnabled($Process.ProcessId);
      if ($dep -eq 0) {
        "DepEnabled=false"
      } elseif ($dep -eq 1) {
        "DepEnabled=true"
      }
    } catch {}
    $title = [jOVAL.Process.Probe]::GetWindowText($Process.ProcessId);
    if ($title -ne $null) {
      "PrimaryWindowText={0}" -f $title
    }
  }
}
