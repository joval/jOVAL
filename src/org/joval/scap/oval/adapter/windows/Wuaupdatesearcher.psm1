# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-WuaUpdates {
  Param (
    [string]$searchCriteria = $(throw "Mandatory parameter -searchCriteria"),
    [boolean]$includeSuperseded = $false
  )

  $ErrorActionPreference = "Continue"
  $wua = New-Object -ComObject Microsoft.Update.Session
  $wusearcher = $wua.CreateUpdateSearcher()
  $results = $wusearcher.Search($searchCriteria)
  foreach($update in $results.Updates) {
    Write-Output $update.Identity.UpdateId
    if ($includeSuperseded) {
      foreach($supersededId in $update.SupersededUpdateIDs) {
        Write-Output $supersededId
      }
    }
  }
}
