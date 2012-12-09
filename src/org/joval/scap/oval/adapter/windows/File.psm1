# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Print-FileInfoEx {
  param(
    [string]$Path = $(throw "Mandatory parameter -Path")
  )

  $code = @"
using System;
using System.Runtime.InteropServices;

namespace jOVAL.File {
    public class PEProbe {
	[DllImport("Imagehlp.dll", EntryPoint="MapFileAndCheckSum", ExactSpelling=false,  CharSet=CharSet.Auto, SetLastError=true)]
	static extern uint MapFileAndCheckSum(string Filename, out uint HeaderSum, out uint CheckSum);

	public static string getChecksum(string Path) {
	    uint HeaderSum, CheckSum = 0;
	    uint result = MapFileAndCheckSum(Path, out HeaderSum, out CheckSum);
	    if (result != 0) {
		throw new System.ComponentModel.Win32Exception((int)result);
	    }
	    return CheckSum.ToString();
	}
    }
}
"@


  $ErrorActionPreference = "SilentlyContinue"
  $type = [jOVAL.File.PEProbe]

  $ErrorActionPreference = "Stop"
  if($type -eq $null){
    add-type $code
  }

  $ErrorActionPreference = "Continue"

  $Info = [System.Diagnostics.FileVersionInfo]::GetVersionInfo($Path)
  Write-Output "FileMajorPart: $($Info.FileMajorPart)"
  Write-Output "FileMinorPart: $($Info.FileMinorPart)"
  Write-Output "FileBuildPart: $($Info.FileBuildPart)"
  Write-Output "FilePrivatePart: $($Info.FilePrivatePart)"
  Write-Output "FileVersion: $($Info.FileVersion)"
  Write-Output "Company Name: $($Info.CompanyName)"
  Write-Output "Internal Name: $($Info.InternalName)"
  Write-Output "Language: $($Info.Language)"
  Write-Output "OriginalFilename: $($Info.OriginalFilename)"
  Write-Output "Product Name: $($Info.ProductName)"
  Write-Output "Product Version: $($Info.ProductVersion)"

  $CheckSum = [jOVAL.File.PEProbe]::getChecksum($Path)
  Write-Output "MSChecksum: $($CheckSum)"
}
