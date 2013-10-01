# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-AuditedPermissions {
  param(
    [Parameter(ValueFromPipeline=$true)][PSObject]$Object = $null
  )

  PROCESS {
    if ($Object -eq $null) {
      throw "Object is NULL"
    }
    $ObjectType = $Object.GetType().Name
    switch -regex ($ObjectType) {
      "RegistryKey" {
        "{"
        "Path: {0}" -f $Object.Name
        $Security = $Object.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Audit)
        foreach($Ace in $Security.GetAuditRules($True, $False, [System.Security.Principal.SecurityIdentifier])) {
          $Mask = [Convert]::ToInt32($Ace.RegistryRights)
          $Flags = [Convert]::ToInt32($Ace.AuditFlags)
          "{0}: {1}, {2}" -f $Ace.IdentityReference, $Mask, $Flags
        }
        "}"
      }
      "(File|Directory)Info" {
        "{"
        "Path: {0}" -f $Object.FullName
        $Security = $Object.GetAccessControl([System.Security.AccessControl.AccessControlSections]::Audit)
        foreach($Ace in $Security.GetAuditRules($True, $False, [System.Security.Principal.SecurityIdentifier])) {
          $Mask = [Convert]::ToInt32($Ace.FileSystemRights)
          $Flags = [Convert]::ToInt32($Ace.AuditFlags)
          "{0}: {1}, {2}" -f $Ace.IdentityReference, $Mask, $Flags
        }
        "}"
      }
      default {
        throw "Unhandled object type: $($ObjectType)"
      }
    }
  }
}
