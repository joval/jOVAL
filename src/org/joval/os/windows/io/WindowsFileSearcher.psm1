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
      if ($Path -match $Pattern) {
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
  param(
    [String]$Path = $PWD,
    [String]$Pattern = ".*",
    [int]$Depth = 1 
  )
  try {
    $CurrentItem = Get-Item -literalPath $Path
    if ($Path -match $Pattern) {
      $CurrentItem
    }
    if ($CurrentItem.PSIsContainer) {
      $NextDepth = $Depth - 1
      if ($Depth -ne 0) {
        $ErrorActionPreference = "SilentlyContinue"
        foreach ($ChildItem in Get-ChildItem $CurrentItem) {
          Find-Files -Path $ChildItem.FullName -Pattern $Pattern -Depth $NextDepth
        }
      }
    }
  } catch {}
}

function Print-FileACEInfo {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)]
    [PSObject]$inputObject
  )

  PROCESS {
    if (!($inputObject -eq $null)) {
      $type = $inputObject | Get-Member | %{$_.TypeName}
      if (($type -eq "System.IO.DirectoryInfo") -or ($type -eq "System.IO.FileInfo")) {
        $acl = Get-ACL $inputObject.FullName
        $aces = $acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier])
        foreach ($ace in $aces) {
          $accessMask = $ace.FileSystemRights.value__
          $sid = $ace.IdentityReference.Value
          Write-Output "ACE: mask=$accessMask,sid=$sid"
        }
      }
    }
  }
}

function Print-FileInfo {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)]
    [PSObject]$inputObject
  )

  PROCESS {
    if (!($inputObject -eq $null)) {
      $type = $inputObject | Get-Member | %{$_.TypeName}
      if ($type -eq "System.IO.DirectoryInfo") {
        Write-Output "{"
        Write-Output "Type: Directory"
	$path = $inputObject.FullName
        Write-Output "Path: $path"
        $ctime = $inputObject.CreationTimeUtc.toFileTimeUtc()
        $mtime = $inputObject.LastWriteTimeUtc.toFileTimeUtc()
        $atime = $inputObject.LastAccessTimeUtc.toFileTimeUtc()
        Write-Output "Ctime: $ctime"
        Write-Output "Mtime: $mtime"
        Write-Output "Atime: $atime"
        Print-FileACEInfo $inputObject
        Write-Output "}"
      } else {
        if ($type -eq "System.IO.FileInfo") {
          Write-Output "{"
          Write-Output "Type: File"
	  $path = $inputObject.FullName
          Write-Output "Path: $path"
          $ctime = $inputObject.CreationTimeUtc.toFileTimeUtc()
          $mtime = $inputObject.LastWriteTimeUtc.toFileTimeUtc()
          $atime = $inputObject.LastAccessTimeUtc.toFileTimeUtc()
          Write-Output "Ctime: $ctime"
          Write-Output "Mtime: $mtime"
          Write-Output "Atime: $atime"
          $length = $inputObject.Length
          Write-Output "Length: $length"
          Print-FileACEInfo $inputObject
          Write-Output "}"
        }
      }
    }
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
