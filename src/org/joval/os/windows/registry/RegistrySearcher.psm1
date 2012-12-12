# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Find-RegKeys {
  param(
    [String]$Hive = "HKEY_LOCAL_MACHINE",
    [String]$Key = "",
    [String]$Pattern = ".*",
    [String]$WithLiteralVal = "",
    [String]$WithValPattern = "",
    [int]$Depth = 1 
  )

  try {
    if ($Key.Length -eq 0) {
      $FullPath = "Registry::$Hive"
    } else {
      $FullPath = "Registry::$Hive\$Key"
    }
    $CurrentKey = Get-Item -literalPath $FullPath
    if ($CurrentKey -ne $null) {
      $NextDepth = $Depth - 1
      if ($Key -imatch $Pattern) {
        if ($WithLiteralVal -ne "") {
          foreach ($ValName in $CurrentKey.GetValueNames()) {
            if ($ValName -eq $WithLiteralVal) {
              $CurrentKey
              break
            }
          }
        } else {
          if ($WithValPattern -ne "") {
            foreach ($ValName in $CurrentKey.GetValueNames()) {
              if ($ValName -imatch $WithValPattern) {
                $CurrentKey
                break
              }
            }
          } else {
            $CurrentKey
          }
        }
      }
      if ($Depth -ne 0) {
        $ErrorActionPreference = "SilentlyContinue"
        foreach ($SubKeyName in $CurrentKey.GetSubKeyNames()) {
          if ($Key.Length -eq 0) {
            $SubKeyPath = $SubKeyName
          } else {
            $SubKeyPath = $Key + "\" + $SubKeyName
          }
          Find-RegKeys -Hive $Hive -Key $SubKeyPath -Pattern $Pattern -WithLiteralVal $WithLiteralVal -WithValPattern $WithValPattern -Depth $NextDepth
        }
      }
    }
  } catch {}
}
