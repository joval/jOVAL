# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Print-RegValues {
  param(
    [String]$Hive = "HKEY_LOCAL_MACHINE",
    [String]$Key = ""
  )

  if ($Key.Length -eq 0) {
    $FullPath = "Registry::$Hive"
  } else {
    $FullPath = "Registry::$Hive\$Key"
  }
  $CurrentKey = Get-Item -literalPath $FullPath
  foreach ($Name in $CurrentKey.GetValueNames()) {
    Write-Output "{"
    Write-Output "Name: $($Name)"
    $Kind = $CurrentKey.GetValueKind($Name)
    Write-Output "Kind: $($Kind)"
    if ("Binary" -eq $Kind) {
      Write-Output "Data: $([System.Convert]::ToBase64String($CurrentKey.GetValue($Name)))"
    } else {
      if ("ExpandString" -eq $Kind) {
        Write-Output "Data: $($CurrentKey.GetValue($Name, $null, 'DoNotExpandEnvironmentNames'))"
      } else {
        if ("MultiString" -eq $Kind) {
          foreach ($Val in $CurrentKey.GetValue($Name)) {
            Write-Output "Data: $($Val)"
          }
        } else {
          Write-Output "Data: $($CurrentKey.GetValue($Name))"
        }
      }
    }
    Write-Output "}"
  }
}
