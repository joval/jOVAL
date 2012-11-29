# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-RegkeyEffectiveRights {
  param(
    [string]$literalPath=$(throw "Mandatory parameter -literalPath missing.")
  )

  $ErrorActionPreference = "Continue"
  $Key = Get-Item -literalPath $literalPath
  $Security = $Key.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Access)
  foreach($ACE in $Security.GetAccessRules($True, $True, [System.Security.Principal.SecurityIdentifier])) {
    $Rights = [Convert]::ToInt32($ACE.RegistryRights)
    Write-Output "$($ACE.IdentityReference): $($Rights)"
  }
}
