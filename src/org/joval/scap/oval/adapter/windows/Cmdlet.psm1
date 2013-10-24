# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-ModuleInfo {
  param(
    [String]$moduleName=$null
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
      "ModuleName={0}" -f $name
      if ($loaded -contains $module) {
          "Status=loaded"
      } else {
        "Status=not loaded"
      }
      Write-Output(Get-Content $module.Path)
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
        $members = $inputObject | Get-Member -MemberType Property, NoteProperty
        foreach ($member in $members) {
          $name = $member.Name
          $object = $inputObject.$name
          $field = "<field name=`"" + $name.ToLower() + "`""
          if ($object -eq $null) {
            $field = "<field name=`"{0}`"/>" -f $name
          } else {
            if ($member.Definition.StartsWith("System.Int32 ")) {
              $datatype = "integer"
            } elseif ($member.Definition.StartsWith("System.Int64 ")) {
              $datatype = "integer"
            } elseif ($member.Definition.StartsWith("System.Boolean ")) {
              $datatype = "boolean"
            } else {
              $datatype = "string"
            }
            $field = "<field name=`"{0}`" datatype=`"{1}`">{2}</field>" -f $name.ToLower(), $datatype, $object.ToString()
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
