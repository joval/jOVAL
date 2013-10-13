# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-EffectiveRights {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)][String]$SID,
    [String]$ObjectType = $(throw "Mandatory parameter -ObjectType"),
    [String]$Name = $(throw "Mandatory parameter -Name")
  )

  PROCESS {
    $ErrorActionPreference = "Continue"
    switch($ObjectType) {
      "File" {
        "{0}: {1:D}" -f $SID, [jOVAL.EffectiveRights.Probe]::GetFileEffectiveRights($Name, $SID)
      } 
      "Printer" {
        "{0}: {1:D}" -f $SID, [jOVAL.EffectiveRights.Probe]::GetPrinterEffectiveRights($Name, $SID)
      }
      "RegKey" {
        "{0}: {1:D}" -f $SID, [jOVAL.EffectiveRights.Probe]::GetRegKeyEffectiveRights($Name, $SID)
      }
      "Service" {
        "{0}: {1:D}" -f $SID, [jOVAL.EffectiveRights.Probe]::GetServiceEffectiveRights($Name, $SID)
      }
      "Share" {
        "{0}: {1:D}" -f $SID, [jOVAL.EffectiveRights.Probe]::GetShareEffectiveRights($Name, $SID)
      }
    }
  }
}
