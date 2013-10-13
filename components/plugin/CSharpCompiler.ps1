# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt
#
function Compile {
    param (
      [String]$Source = $(throw "Mandatory parameter -Source missing."),
      [String]$Output = $(throw "Mandatory parameter -Output missing.")
    )

    $Params = New-Object System.CodeDom.Compiler.CompilerParameters
    $Params.ReferencedAssemblies.Add("System.dll");
    $Params.ReferencedAssemblies.Add("System.serviceprocess.dll");
    $Params.GenerateExecutable = $false
    $Params.IncludeDebugInformation = $false
    $Params.WarningLevel = 3
    $Params.TreatWarningsAsErrors = $false
    $Params.CompilerOptions = "/optimize"
    $Params.OutputAssembly = $Output
    $Params.GenerateInMemory = $false
    $Temp = $(Get-Item ENV:TEMP).Value
    $Params.TempFiles = New-Object System.CodeDom.Compiler.TempFileCollection $Temp, $true
    
    $Provider = New-Object Microsoft.CSharp.CSharpCodeProvider
    Try {
	$CompilerResults = $Provider.CompileAssemblyFromFile($Params, $Source)
	if ($CompilerResults.Errors.Count -gt 0) {
	    $CodeLines = $(Get-Content $Source) -Split '[\n]'
	    $ErrorMessage = "Compilation Errors:"
	    $errnum = 1;
	    foreach ($CompileError in $CompilerResults.Errors) {
		$ErrorMessage = [String]::Concat($errnum.ToString(), $ErrorMessage, "`n", $CompileError.ToString())
		$errnum++
	    }
	    Write-Error $ErrorMessage
	} else {
	    Write-Output "Compiled successfully to $($Output)"
	}
    } Catch [System.Exception] {
	if ($_.GetType().ToString() -eq "System.Management.Automation.ErrorRecord") {
	    Write-Error $_.Exception.ToString()
	} else {
	    Write-Error $_.ToString()
	}
    }
}

Compile -Source "..\..\src\org\joval\scap\oval\adapter\independent\Environmentvariable58.cs" -Output "rsrc\assembly\Environmentvariable58.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\independent\SHA224.cs" -Output "rsrc\assembly\SHA224.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Accesstoken.cs" -Output "rsrc\assembly\Accesstoken.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Auditeventpolicy.cs" -Output "rsrc\assembly\Auditeventpolicy.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Effectiverights.cs" -Output "rsrc\assembly\Effectiverights.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Lockoutpolicy.cs" -Output "rsrc\assembly\Lockoutpolicy.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Metabase.cs" -Output "rsrc\assembly\Metabase.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Process.cs" -Output "rsrc\assembly\Process.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Registry.cs" -Output "rsrc\assembly\Registry.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Service.cs" -Output "rsrc\assembly\Service.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Sharedresource.cs" -Output "rsrc\assembly\Sharedresource.dll"
Compile -Source "..\..\src\org\joval\scap\oval\adapter\windows\Systemmetric.cs" -Output "rsrc\assembly\Systemmetric.dll"
