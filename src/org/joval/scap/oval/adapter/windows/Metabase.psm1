# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt
#
function Find-MetabaseKeys {
  param(
    [String]$Path = "/",
    [String]$Pattern = ".*"
  )
  foreach ($Subkey in [jOVAL.Metabase.Probe]::ListSubkeys($Path)) {
    if ($Path -eq "/") {
      $Newpath = $Subkey
    } else {
      $Newpath = $Path + "/" + $Subkey
    }
    if ($Newpath -match $Pattern) {
      "/{0}" -f $Newpath
    }
    Find-MetabaseKeys -Path $Newpath -Pattern $Pattern
  }
}

function Get-MetabaseData {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)][String]$KeyPath,
    [Int32]$ID = $null
  )
  PROCESS {
    if (!($KeyPath -eq $null) -and [jOVAL.Metabase.Probe]::TestKey($KeyPath)) {
      "Key: {0}" -f $KeyPath
      if ($ID -eq $null -or $ID -eq "") {
        foreach ($Datum in [jOVAL.Metabase.Probe]::ListData($KeyPath)) {
	  "{"
          foreach ($Entry in $Datum.GetEnumerator()) {
	    "{0}: {1}" -f $Entry.Key.ToString(), $Entry.Value
          }
	  "}"
        }
      } else {
	$Datum = [jOVAL.Metabase.Probe]::GetData($KeyPath, $ID);
	if ($Datum -ne $null) {
	  foreach ($Entry in $Datum.GetEnumerator()) {
	    "{0}: {1}" -f $Entry.Key.ToString(), $Entry.Value
	  }
	}
      }
    }
  }
}
