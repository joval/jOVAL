// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace Registry {
	[StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
	public struct FILETIME {
	    public Int32 LowDateTime;
	    public Int32 HighDateTime;
	}

	public class Probe {
	    public const int KEYQUERYVALUE = 0x1;
	    public const int KEYREAD = 0x19;
	    public const int KEYALLACCESS = 0x3F;

	    [DllImport("advapi32.dll", CharSet = CharSet.Auto)]
	    public static extern int RegOpenKeyEx(int hKey, String subKey, int ulOptions, int samDesired, ref UIntPtr hkResult);

	    [DllImport("advapi32.dll", EntryPoint="RegQueryInfoKey", CallingConvention=CallingConvention.Winapi,
		 SetLastError=true)]
	    public static extern int RegQueryInfoKey(UIntPtr hkey, out StringBuilder lpClass, ref IntPtr lpcbClass,
		 IntPtr lpReserved, out uint lpcSubKeys, out uint lpcbMaxSubKeyLen, out uint lpcbMaxClassLen,
		 out uint lpcValues, out uint lpcbMaxValueNameLen, out uint lpcbMaxValueLen, out uint lpcbSecurityDescriptor,
		 ref FILETIME lastWriteTime);

	    [DllImport("advapi32.dll", SetLastError=true)]
	    public static extern int RegCloseKey(UIntPtr hKey);

	    public static DateTime GetLastWriteTime(int hive, String subkey) {
		UIntPtr hKey = UIntPtr.Zero;
		int result = RegOpenKeyEx(hive, subkey, 0, KEYREAD, ref hKey);
		if (result != 0) {
		    throw new System.ComponentModel.Win32Exception(result);
		}

		StringBuilder lpClass = new StringBuilder(1024);
		IntPtr lpcbClass = IntPtr.Zero;
		IntPtr lpReserved = IntPtr.Zero;
		uint lpcSubKeys;
		uint lpcbMaxSubKeyLen;
		uint lpcbMaxClassLen;
		uint lpcValues;
		uint lpcbMaxValueNameLen;
		uint lpcbMaxValueLen;
		uint lpcbSecurityDescriptor;
		FILETIME lastWriteTime = new FILETIME();
		result = RegQueryInfoKey(hKey, out lpClass, ref lpcbClass, lpReserved, out lpcSubKeys, out lpcbMaxSubKeyLen,
					 out lpcbMaxClassLen, out lpcValues, out lpcbMaxValueNameLen, out lpcbMaxValueLen,
					 out lpcbSecurityDescriptor, ref lastWriteTime);
		RegCloseKey(hKey);
		if (result != 0) {
		    throw new System.ComponentModel.Win32Exception(result);
		}

		byte[] high = BitConverter.GetBytes(lastWriteTime.HighDateTime);
		byte[] low = BitConverter.GetBytes(lastWriteTime.LowDateTime);
		byte[] buff = new byte[high.Length + low.Length];
		Buffer.BlockCopy(low, 0, buff, 0, low.Length );
		Buffer.BlockCopy(high, 0, buff, low.Length, high.Length );
		long time = BitConverter.ToInt64(buff, 0);
		return DateTime.FromFileTimeUtc(time);
	    }
	}
    }
}
