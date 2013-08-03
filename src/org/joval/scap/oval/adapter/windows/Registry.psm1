# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-RegKeyLastWriteTime {
  param (
    [Parameter(ValueFromPipeline=$true)]
    [String]$Subkey = $null,
    [String]$Hive = "HKEY_LOCAL_MACHINE"
  )

  BEGIN {
    switch ($Hive) {
      "HKEY_CLASSES_ROOT" { $hKey = 0x80000000}
      "HKEY_CURRENT_USER" { $hKey = 0x80000001}
      "HKEY_LOCAL_MACHINE" { $hKey = 0x80000002}
      "HKEY_USERS"  { $hKey = 0x80000003}
      "HKEY_CURRENT_CONFIG" { $hKey = 0x80000005}
      default { 
        throw "Invalid Hive: $($Hive)"
      }
    }
  }

  PROCESS {
    if ($Subkey -eq $null -or $Subkey -eq "") {
      $Path = $Hive
    } else {
      $Path = "$($Hive)\$($Subkey)"
    }
    if (Test-Path -LiteralPath "Registry::$($Path)") {
      "{0}: {1:D}" -f $Path, [jOVAL.Registry.Probe]::GetLastWriteTime($hKey, $Subkey).ToFileTimeUtc()
    }
  }
}
