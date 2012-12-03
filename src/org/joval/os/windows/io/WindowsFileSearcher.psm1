# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Find-Directories {
  param(
    [String]$Path = $PWD,
    [String]$Pattern = ".*",
    [int]$Depth = 1 
  )

  try {
    $CurrentItem = Get-Item -literalPath $Path
    if (($CurrentItem -ne $null) -and $CurrentItem.PSIsContainer) {
      $NextDepth = $Depth - 1
      if ($Path -cmatch $Pattern) {
	$CurrentItem
      }
      if ($Depth -ne 0) {
	$ErrorActionPreference = "SilentlyContinue"
	foreach ($ChildItem in Get-ChildItem $CurrentItem) {
	  if ($ChildItem.PSIsContainer) {
	    Find-Directories -Path $ChildItem.FullName -Pattern $Pattern -Depth $NextDepth
	  }
	}
      }
    }
  } catch {}
}

function Find-Files {
  [CmdletBinding(DefaultParameterSetName="Pattern")]
  param(
    [Parameter(ValueFromPipeline=$true)][PSObject]$CurrentItem,
    [String]$Path = $PWD,
    [String]$Pattern = ".*",
    [int]$Depth = 1,
    [Parameter(ParameterSetName="Pattern")][String]$Filename = ".*",
    [Parameter(ParameterSetName="Literal")][String]$LiteralFilename = ".*"
  )

  PROCESS {
    if ($CurrentItem -eq $null) {
      $CurrentItem = Get-Item -literalPath $Path
    }
    try {
      if ($Pattern -eq ".*") {
	if ($PsCmdlet.ParameterSetName -eq "Pattern") {
	  if ($Filename -eq ".*") {
	    $CurrentItem
	  } else {
	    if (!$CurrentItem.PSIsContainer -and ($CurrentItem.Name -cmatch $Filename)) {
	      $CurrentItem
	    }
	  }
	} else {
	  $Name = $CurrentItem.Name
	  if (!$CurrentItem.PSIsContainer -and ($Name -eq $LiteralFilename)) {
	    $CurrentItem
	  }
	}
      } else {
	if ($Path -cmatch $Pattern) {
	  $CurrentItem
	}
      }
      if ($CurrentItem.PSIsContainer) {
	$NextDepth = $Depth - 1
	if ($Depth -ne 0) {
	  $ErrorActionPreference = "SilentlyContinue"
	  foreach ($ChildItem in Get-ChildItem $CurrentItem) {
	    if ($Pattern -eq ".*") {
	      if ($PsCmdlet.ParameterSetName -eq "Pattern") {
		if ($Filename -eq ".*") {
		  Find-Files -Path $ChildItem.FullName -Depth $NextDepth
		} else {
		  Find-Files -Path $ChildItem.FullName -Filename $Filename -Depth $NextDepth
		}
	      } else {
		  Find-Files -Path $ChildItem.FullName -LiteralFilename $LiteralFilename -Depth $NextDepth
	      }
	    } else {
	      Find-Files -Path $ChildItem.FullName -Pattern $Pattern -Depth $NextDepth
	    }
	  }
	}
      }
    } catch {}
  }
}

function Gzip-File {
  param (
    [String]$in = $(throw "Mandatory parameter -in missing."),
    [String]$out = $($in + ".gz")
  )
 
  if (Test-Path $in) {
    $input = New-Object System.IO.FileStream $in, ([IO.FileMode]::Open), ([IO.FileAccess]::Read), ([IO.FileShare]::Read)
    $output = New-Object System.IO.FileStream $out, ([IO.FileMode]::Create), ([IO.FileAccess]::Write), ([IO.FileShare]::None)
    $gzipStream = New-Object System.IO.Compression.GzipStream $output, ([IO.Compression.CompressionMode]::Compress)

    $buffer = New-Object byte[](512)
    $len = 0
    while(($len = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
      $gzipStream.Write($buffer, 0, $len)
    }
    $input.Close()
    $gzipStream.Close()
    $output.Close()
    Remove-Item $in
  }
}
