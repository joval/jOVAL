# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-AccessTokens {
  param(
    [string]$Principal=$(throw "Mandatory parameter -Principal missing.")
  )

  if ($Principal -eq $null) {
    throw "Principal is NULL"
  }
 
  $code = @"
using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.InteropServices;

namespace jOVAL.AccessToken {
    public class Probe {
	public const int POLICY_VIEW_LOCAL_INFORMATION = 0x1;
	public const int POLICY_LOOKUP_NAMES = 0x00000800;
	public const int POLICY_ALL_ACCESS = 0x00F0FFF;

	[DllImport("kernel32.dll")]
	extern static int GetLastError();

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
	public static extern UInt32 LsaNtStatusToWinError(UInt32 Status);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true, PreserveSig = true)]
	public static extern bool ConvertStringSidToSid(string StringSid, out IntPtr pSid);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true, PreserveSig = true)]
	public static extern bool LookupAccountName(string lpSystemName, string lpAccountName, IntPtr psid, ref int cbsid, StringBuilder domainName, ref int cbdomainLength, ref int use);

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

	public static LSA_UNICODE_STRING string2LSAUS(string myString) {
	    LSA_UNICODE_STRING retStr = new LSA_UNICODE_STRING();
	    retStr.Buffer = Marshal.StringToHGlobalUni(myString);
	    retStr.Length = (UInt16)(myString.Length * UnicodeEncoding.CharSize);
	    retStr.MaximumLength = (UInt16)((myString.Length + 1) * UnicodeEncoding.CharSize);
	    return retStr;
	}

	public static string LSAUS2string(LSA_UNICODE_STRING lsaus) {
	    char[] cvt = new char[lsaus.Length / UnicodeEncoding.CharSize];
	    Marshal.Copy(lsaus.Buffer, cvt, 0, lsaus.Length / UnicodeEncoding.CharSize);
	    return new string(cvt);
	}

	public static List<string> getAccessTokens(string entityName) {
	    List<string> retList = new List<string>();
	    IntPtr sid = IntPtr.Zero;
	    int sidSize = 0;
	    StringBuilder domainName = new StringBuilder();
	    int nameSize = 0;
	    int accountType = 0;
	    UInt32 lretVal;
	    uint retVal;

	    LookupAccountName(null, entityName, sid, ref sidSize, domainName, ref nameSize, ref accountType);
	    domainName = new StringBuilder(nameSize);
	    sid = Marshal.AllocHGlobal(sidSize);

	    if (!LookupAccountName(null, entityName, sid, ref sidSize, domainName, ref nameSize, ref accountType)) {
		Marshal.FreeHGlobal(sid);
		throw new System.ComponentModel.Win32Exception(GetLastError());
	    }

	    IntPtr policyHandle = IntPtr.Zero;
	    LSA_OBJECT_ATTRIBUTES objAttrs = new LSA_OBJECT_ATTRIBUTES();
	    int mode = POLICY_LOOKUP_NAMES | POLICY_VIEW_LOCAL_INFORMATION;
	    lretVal = LsaOpenPolicy(null, ref objAttrs, mode, out policyHandle);
	    retVal = LsaNtStatusToWinError(lretVal);

	    if (retVal != 0) {
		Marshal.FreeHGlobal(sid);
		throw new System.ComponentModel.Win32Exception((int)retVal);
	    }

	    IntPtr rightsArray = IntPtr.Zero;
	    UInt32 rightsCount = 0;
	    lretVal = LsaEnumerateAccountRights(policyHandle, sid, out rightsArray, out rightsCount);
	    retVal = LsaNtStatusToWinError(lretVal);
	    Marshal.FreeHGlobal(sid);

	    if (retVal == 2) {
		LsaClose(policyHandle);
		return retList;
	    }
	    if (retVal != 0) {
		LsaClose(policyHandle);
		throw new System.ComponentModel.Win32Exception((int)retVal);
	    }

	    LSA_UNICODE_STRING myLsaus = new LSA_UNICODE_STRING();
	    for (ulong i = 0; i < rightsCount; i++) {
		IntPtr itemAddr = new IntPtr(rightsArray.ToInt64() + (long)(i * (ulong)Marshal.SizeOf(myLsaus)));
		myLsaus = (LSA_UNICODE_STRING)Marshal.PtrToStructure(itemAddr, myLsaus.GetType());
		string thisRight = LSAUS2string(myLsaus);
		retList.Add(thisRight);
	    }

	    lretVal = LsaClose(policyHandle);
	    retVal = LsaNtStatusToWinError(lretVal);
	    if (retVal != 0) {
		throw new System.ComponentModel.Win32Exception((int)retVal);
	    }

	    return retList;
	}
    }
}
"@

  $ErrorActionPreference = "SilentlyContinue" 
  $type = [jOVAL.AccessToken.Probe]
  if($type -eq $null){
    add-type $code
  }

  $ErrorActionPreference = "Continue" 
  $result = [jOVAL.AccessToken.Probe]::getAccessTokens($Principal)
  foreach($token in $result) {
    Write-Output $token
  }
}
