# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Print-RegkeyAccess {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)]
    [PSObject]$inputObject
  )

  PROCESS {
    if (!($inputObject -eq $null)) {
      $type = $inputObject | Get-Member | %{$_.TypeName}
      if ($type -eq "Microsoft.Win32.RegistryKey") {
        $acl = $inputObject.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Access)
	foreach ($ace in $acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier])) {
          $accessMask = [Convert]::ToInt32($ace.RegistryRights)
	  $sid = $ace.IdentityReference.Value
	  Write-Output "ACE: mask=$accessMask,sid=$sid"
	}
      }
    }
  }
}