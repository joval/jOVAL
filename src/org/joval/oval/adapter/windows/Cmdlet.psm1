# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-ModuleInfo {
  param(
    [string]$moduleName=$null
  )

  if ($moduleName -eq $null) {
    $available = Get-Module -listAvailable
  } else {
    $available = Get-Module -listAvailable -Name $moduleName
  }

  $loaded = Get-Module
  foreach ($module in $available) {
    if ($module.Path.EndsWith(".psd1")) {
      $name = $module.Name
      Write-Output "ModuleName=$name"
      if ($loaded -contains $module) {
          Write-Output "Status=loaded"
      } else {
        Write-Output "Status=not loaded"
      }
      $content = Get-Content $module.Path
      Write-Output $content
    }
  }
}

function ConvertTo-OVAL {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)]
    [PSObject[]]$inputObjects
  )

  BEGIN {
    $item = "<win-sc:cmdlet_item xmlns=`"http://oval.mitre.org/XMLSchema/oval-system-characteristics-5`" "
    $item += "xmlns:win-sc=`"http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#windows`">"
  }

  PROCESS {
    if (!($inputObjects -eq $null)) {
      for ($i=0; $i -lt $inputObjects.count; $i++) {
        $inputObject = $inputObjects[$i]
        $value = "<win-sc:value datatype=`"record`">"
        $members = $inputObject | Get-Member -MemberType Property
        foreach ($member in $members) {
          $name = $member.Name
          $object = $inputObject.$name
          $field = "<field name=`"" + $name + "`""
          if ($object -eq $null) {
            $field += "/>"
          } else {
            if ($member.Definition.StartsWith("System.Int32 ")) {
              $field += " datatype=`"integer`">" + $object + "</field>"
            } elseif ($member.Definition.StartsWith("System.Int64 ")) {
              $field += " datatype=`"integer`">" + $object + "</field>"
            } elseif ($member.Definition.StartsWith("System.Boolean ")) {
              $field += " datatype=`"boolean`">" + $object + "</field>"
            } elseif ($member.Definition.startsWith("System.String ")) {
              $field += " datatype=`"string`">" + $object + "</field>"
            } else {
              $field += " datatype=`"string`">" + $object.ToString() + "</field>"
            }
          }
          $value += $field
        }
        $value += "</win-sc:value>"
        $item += $value
      }
    }
  }

  END {
    $item += "</win-sc:cmdlet_item>"
    return $item
  }
}
