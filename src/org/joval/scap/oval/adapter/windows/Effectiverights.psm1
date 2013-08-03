# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-EffectiveRights {
  param(
    [String]$ObjectType = $(throw "Mandatory parameter -ObjectType"),
    [String]$Name = $(throw "Mandatory parameter -Name"),
    [String]$SID = $(throw "Mandatory parameter -SID")
  )

  $ErrorActionPreference = "Continue"
  switch($ObjectType) {
    "File"    {"{0:D}" -f [jOVAL.EffectiveRights.Probe]::GetFileEffectiveRights($Name, $SID)} 
    "RegKey"  {"{0:D}" -f [jOVAL.EffectiveRights.Probe]::GetRegKeyEffectiveRights($Name, $SID)}
    "Service" {"{0:D}" -f [jOVAL.EffectiveRights.Probe]::GetServiceEffectiveRights($Name, $SID)}
  }
}
