# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-FileAuditedPermissions {
  param(
    [string]$Path=$(throw "Mandatory parameter -Path missing.")
  )

  if ($Path -eq $null) {
    throw "Path is NULL"
  }

  $ErrorActionPreference = "Continue" 

  $Query = "Select * FROM Win32_LogicalFileSecuritySetting Where Path='" + $Path + "'"
  $SD = Get-WmiObject -Query $Query | Invoke-WmiMethod -Name GetSecurityDescriptor
  foreach ($SACL in $SD.Descriptor.SACL) {
    if (!($SACL -eq $null)) {
      Write-Output ($SACL.Trustee.SIDString + ": " + $SACL.AccessMask + ", " + $SACL.AceFlags)
    }
  }
}
