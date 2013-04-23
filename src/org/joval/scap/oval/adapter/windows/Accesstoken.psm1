# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-AccessTokens {
  $Source = @"
using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.InteropServices;

namespace jOVAL.AccessToken {
  public class Probe {
    public const int ERROR_SUCCESS = 0;
    public const int ERROR_FILE_NOT_FOUND = 0x2;

    public const int POLICY_VIEW_LOCAL_INFORMATION = 0x1;
    public const int POLICY_LOOKUP_NAMES = 0x00000800;
    public const int POLICY_ALL_ACCESS = 0x00F0FFF;

    [DllImport("kernel32.dll")]
    extern static int GetLastError();

    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
    public static extern UInt32 LsaNtStatusToWinError(UInt32 Status);

    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true, PreserveSig = true)]
    public static extern bool ConvertStringSidToSid(string StringSid, out IntPtr pSid);

    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
    public static extern UInt32 LsaOpenPolicy(string SystemName, ref LSA_OBJECT_ATTRIBUTES ObjectAttributes, Int32 DesiredAccess, out IntPtr PolicyHandle);

    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
    public static extern UInt32 LsaClose(IntPtr PolicyHandle);

    [DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
    public static extern UInt32 LsaEnumerateAccountRights(IntPtr PolicyHandle, IntPtr AccountSid, out IntPtr UserRights, out UInt32 CountOfRights);

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct LSA_UNICODE_STRING {
      public UInt16 Length;
      public UInt16 MaximumLength;
      public IntPtr Buffer;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct LSA_OBJECT_ATTRIBUTES {
      public IntPtr RootDirectory;
      public IntPtr SecurityDescriptor;
      public IntPtr SecurityQualityOfService;
      public LSA_UNICODE_STRING ObjectName;
      public UInt32 Attributes;
      public UInt32 Length;
    }

    public static string LSAUS2string(LSA_UNICODE_STRING lsaus) {
      char[] cvt = new char[lsaus.Length / UnicodeEncoding.CharSize];
      Marshal.Copy(lsaus.Buffer, cvt, 0, lsaus.Length / UnicodeEncoding.CharSize);
      return new string(cvt);
    }

    public static List<string> getAccessTokens(string sidString) {
      List<string> tokens = new List<string>();
      IntPtr sid = IntPtr.Zero;
      if (!ConvertStringSidToSid(sidString, out sid)) {
	int errorCode = GetLastError();
	switch(errorCode) {
	  case ERROR_FILE_NOT_FOUND: // no such user?
	    return tokens;
	  default:
	    throw new System.ComponentModel.Win32Exception(errorCode);
	}
      }

      IntPtr policyHandle = IntPtr.Zero;
      LSA_OBJECT_ATTRIBUTES objAttrs = new LSA_OBJECT_ATTRIBUTES();
      int mode = POLICY_LOOKUP_NAMES | POLICY_VIEW_LOCAL_INFORMATION;
      uint result = LsaNtStatusToWinError(LsaOpenPolicy(null, ref objAttrs, mode, out policyHandle));
      if (result != 0) {
	Marshal.FreeHGlobal(sid);
	throw new System.ComponentModel.Win32Exception((int)result);
      }

      IntPtr rightsArray = IntPtr.Zero;
      UInt32 rightsLen = 0;
      result = LsaNtStatusToWinError(LsaEnumerateAccountRights(policyHandle, sid, out rightsArray, out rightsLen));
      Marshal.FreeHGlobal(sid);
      switch((int)result) {
	case ERROR_SUCCESS:
	  LSA_UNICODE_STRING myLsaus = new LSA_UNICODE_STRING();
	  for (ulong i=0; i < rightsLen; i++) {
	    IntPtr itemAddr = new IntPtr(rightsArray.ToInt64() + (long)(i * (ulong)Marshal.SizeOf(myLsaus)));
	    myLsaus = (LSA_UNICODE_STRING)Marshal.PtrToStructure(itemAddr, myLsaus.GetType());
	    string thisRight = LSAUS2string(myLsaus);
	    tokens.Add(thisRight);
	  }
	  break;
	case ERROR_FILE_NOT_FOUND:
	  // no account rights were found for the user
	  break;
	default:
	  LsaClose(policyHandle);
	  throw new System.ComponentModel.Win32Exception((int)result);
      }

      LsaClose(policyHandle);
      return tokens;
    }
  }
}
"@

  $ErrorActionPreference = "SilentlyContinue" 
  $Type = [jOVAL.AccessToken.Probe]
  $ErrorActionPreference = "Stop" 
  if ($Type -eq $null){
    New-Type -TypeDefinition $Source
  }
  $ErrorActionPreference = "Continue" 
  foreach ($SID in $input) {
    $Result = [jOVAL.AccessToken.Probe]::getAccessTokens($SID)
    Write-Output "[$($SID)]"
    foreach ($Token in $Result) {
      Write-Output "$($Token): true"
    }
  }
}
