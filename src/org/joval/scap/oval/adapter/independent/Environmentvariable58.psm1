# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function List-Processes {
  $ErrorActionPreference = "Continue"
  foreach ($Process in Get-Process) {
    $Arch = $null
    try {
      $Arch = [jOVAL.Environmentvariable58.ProcessHelper]::GetProcessArchitecture($Process.Id)
    } catch {
    }
    "{0:D}: {1:D}" -f $Process.Id, $Arch
  }
}

function Get-ProcessEnvironment {
  param(
    [int]$ProcessId=$(throw "Mandatory parameter -ProcessId missing.")
  )

  $ErrorActionPreference = "Continue"
  $Dictionary = [jOVAL.Environmentvariable58.Probe]::GetEnvironmentVariables($ProcessId)
  foreach ($Entry in $Dictionary) {
    if ($Entry -ne $null) {
      "{0}={1}" -f $Entry.Key, $Entry.Value
    }
  }
}
