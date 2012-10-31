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
  $file = Get-Item -literalPath $Path
  $security = $file.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Audit)
  foreach($ace in $security.GetAuditRules($True, $False, [System.Security.Principal.SecurityIdentifier])) {
    $mask = [Convert]::ToInt32($ace.FileSystemRights)
    $flags = [Convert]::ToInt32($ace.AuditFlags)
    Write-Output "$($ace.IdentityReference): $mask, $flags"
  }
}
