# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-RegkeyEffectiveRights {
  param(
    [string]$Path=$(throw "Mandatory parameter -Path missing.")
  )

  $key = Get-Item "Registry::$($Path)"
  $security = $key.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Access)
  foreach($ace in $security.GetAccessRules($True, $True, [System.Security.Principal.SecurityIdentifier])) {
    $rights = [Convert]::ToInt32($ace.RegistryRights)
    Write-Output "$($ace.IdentityReference): $($rights)"
  }
}
