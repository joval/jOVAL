# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Print-FileInfo {
  param(
    [Parameter(Mandatory=$true, Position=0, ValueFromPipeline=$true)]
    [PSObject]$inputObject
  )

  BEGIN {
    $code = @"
using System;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32.SafeHandles;

namespace jOVAL.File {
    public class Probe {
	[Flags]
	public enum FileType : uint {
	    FileTypeUnknown = 0x0000, FileTypeDisk = 0x0001, FileTypeChar = 0x0002, FileTypePipe = 0x0003,
	    FileTypeRemote = 0x8000
	}

	[Flags]
	public enum EFileAccess : uint {
	    AccessSystemSecurity = 0x1000000, MaximumAllowed = 0x2000000,

	    Delete = 0x10000, ReadControl = 0x20000, WriteDAC = 0x40000, WriteOwner = 0x80000, Synchronize = 0x100000,

	    StandardRightsRequired = 0xF0000, StandardRightsRead = ReadControl, StandardRightsWrite = ReadControl,
	    StandardRightsExecute = ReadControl, StandardRightsAll = 0x1F0000, SpecificRightsAll = 0xFFFF,

	    FILE_READ_DATA = 0x0001, FILE_LIST_DIRECTORY = 0x0001, FILE_WRITE_DATA = 0x0002, FILE_ADD_FILE = 0x0002,
	    FILE_APPEND_DATA = 0x0004, FILE_ADD_SUBDIRECTORY = 0x0004, FILE_CREATE_PIPE_INSTANCE = 0x0004,
	    FILE_READ_EA = 0x0008, FILE_WRITE_EA = 0x0010, FILE_EXECUTE = 0x0020, FILE_TRAVERSE = 0x0020,
	    FILE_DELETE_CHILD = 0x0040, FILE_READ_ATTRIBUTES = 0x0080, FILE_WRITE_ATTRIBUTES = 0x0100,

	    GenericRead = 0x80000000, GenericWrite = 0x40000000, GenericExecute = 0x20000000, GenericAll = 0x10000000,

	    SPECIFIC_RIGHTS_ALL = 0x00FFFF,
	    FILE_ALL_ACCESS = StandardRightsRequired | Synchronize | 0x1FF,
	    FILE_GENERIC_READ = StandardRightsRead | FILE_READ_DATA | FILE_READ_ATTRIBUTES | FILE_READ_EA | Synchronize,
	    FILE_GENERIC_WRITE = StandardRightsWrite | FILE_WRITE_DATA | FILE_WRITE_ATTRIBUTES | FILE_WRITE_EA | FILE_APPEND_DATA | Synchronize,
	    FILE_GENERIC_EXECUTE = StandardRightsExecute | FILE_READ_ATTRIBUTES | FILE_EXECUTE | Synchronize
	}

	[Flags]
	public enum EFileShare : uint {
	    None = 0x00000000, Read = 0x00000001, Write = 0x00000002, Delete = 0x00000004
	}

	[Flags]
	public enum ECreationDisposition : uint {
	    New = 1, CreateAlways = 2, OpenExisting = 3, OpenAlways = 4, TruncateExisting = 5
	}

	[Flags]
	public enum EFileAttributes : uint {
	    Readonly = 0x00000001, Hidden = 0x00000002, System = 0x00000004, Directory = 0x00000010,
	    Archive = 0x00000020, Device = 0x00000040, Normal = 0x00000080, Temporary = 0x00000100,
	    SparseFile = 0x00000200, ReparsePoint = 0x00000400, Compressed = 0x00000800, Offline = 0x00001000,
	    NotContentIndexed = 0x00002000, Encrypted = 0x00004000, Write_Through = 0x80000000, Overlapped = 0x40000000,
	    NoBuffering = 0x20000000, RandomAccess = 0x10000000, SequentialScan = 0x08000000, DeleteOnClose = 0x04000000,
	    BackupSemantics = 0x02000000, PosixSemantics = 0x01000000, OpenReparsePoint = 0x00200000,
	    OpenNoRecall = 0x00100000, FirstPipeInstance = 0x00080000
	}

	[DllImport("kernel32.dll")]
	public extern static int GetLastError();

	[DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Auto)]
	public extern static SafeFileHandle CreateFile(string lpFileName, EFileAccess dwDesiredAccess, EFileShare dwShareMode, IntPtr lpSecurityAttributes, ECreationDisposition dwCreationDisposition, EFileAttributes dwFlagsAndAttributes, IntPtr hTemplateFile);

	[DllImport("kernel32.dll")]
	public extern static FileType GetFileType(SafeFileHandle hFile);

	[DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Auto)]
	static extern uint GetLongPathName(string ShortPath, StringBuilder sb, int buffer);

	[DllImport("kernel32.dll")]
	static extern uint GetShortPathName(string longpath, StringBuilder sb, int buffer); 

	public static int GetFileType(string Path) {
	    IntPtr hSecurityAttributes = IntPtr.Zero;
	    IntPtr hTemplate = IntPtr.Zero;
	    SafeFileHandle hFile = CreateFile(Path, EFileAccess.FILE_GENERIC_READ, EFileShare.Read, hSecurityAttributes, ECreationDisposition.OpenExisting, EFileAttributes.Normal, hTemplate);
	    FileType type = GetFileType(hFile);
	    if (hFile.IsInvalid) {
		throw new System.ComponentModel.Win32Exception(GetLastError());
	    }
	    hFile.Close();
	    return (int)type;
	}

	public static string GetWindowsPhysicalPath(string path) {
	    StringBuilder builder = new StringBuilder(255);

	    // names with long extension can cause the short name to be actually larger than
	    // the long name.
	    GetShortPathName(path, builder, builder.Capacity);

	    path = builder.ToString();

	    uint result = GetLongPathName(path, builder, builder.Capacity);

	    if (result > 0 && result < builder.Capacity) {
		//Success retrieved long file name
		builder[0] = char.ToUpper(builder[0]);
		return builder.ToString(0, (int)result);
	    }

	    if (result > 0) {
		//Need more capacity in the buffer
		//specified in the result variable
		builder = new StringBuilder((int)result);
		result = GetLongPathName(path, builder, builder.Capacity);
		builder[0] = char.ToUpper(builder[0]);
		return builder.ToString(0, (int)result);
	    }

	    return null;
	}
    }
}
"@

    $ErrorActionPreference = "SilentlyContinue" 
    $type = [jOVAL.File.Probe]

    $ErrorActionPreference = "Stop" 
    if($type -eq $null){
      add-type $code
    }
  }

  PROCESS {
    if (!($inputObject -eq $null)) {
      $type = $inputObject | Get-Member | %{$_.TypeName}
      if ($type -eq "System.IO.DirectoryInfo") {
	Write-Output "{"
	Write-Output "Type: Directory"
	$path = [jOVAL.File.Probe]::GetWindowsPhysicalPath($inputObject.FullName)
	Write-Output "Path: $path"
	$ctime = $inputObject.CreationTimeUtc.toFileTimeUtc()
	$mtime = $inputObject.LastWriteTimeUtc.toFileTimeUtc()
	$atime = $inputObject.LastAccessTimeUtc.toFileTimeUtc()
	Write-Output "Ctime: $ctime"
	Write-Output "Mtime: $mtime"
	Write-Output "Atime: $atime"
	Write-Output "}"
      } else {
	if ($type -eq "System.IO.FileInfo") {
	  Write-Output "{"
	  Write-Output "Type: File"
	  $path = [jOVAL.File.Probe]::GetWindowsPhysicalPath($inputObject.FullName)
	  $winType = [jOVAL.File.Probe]::GetFileType($path)
	  Write-Output "WinType: $winType"
	  Write-Output "Path: $path"
	  $ctime = $inputObject.CreationTimeUtc.toFileTimeUtc()
	  $mtime = $inputObject.LastWriteTimeUtc.toFileTimeUtc()
	  $atime = $inputObject.LastAccessTimeUtc.toFileTimeUtc()
	  Write-Output "Ctime: $ctime"
	  Write-Output "Mtime: $mtime"
	  Write-Output "Atime: $atime"
	  $length = $inputObject.Length
	  Write-Output "Length: $length"
	  Write-Output "}"
	}
      }
    }
  }
}
