# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-FileHash {
  param(
    [Parameter(ValueFromPipeline=$true)][String]$Path=$null,
    [ValidateSet("MD5", "SHA1", "SHA256", "SHA384", "SHA512")]$Algorithm=$(throw "Mandatory parameter -Algorithm missing.")
  )

  PROCESS {
    $hasher = [System.Security.Cryptography.HashAlgorithm]::Create($Algorithm)
    $hash = $hasher.ComputeHash([System.IO.File]::OpenRead($Path))
    Write-Output([System.Convert]::ToBase64String($hash))
  }
}
