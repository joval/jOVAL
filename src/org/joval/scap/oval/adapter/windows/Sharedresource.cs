// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL.SharedResource {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.ServiceProcess;
    using System.Text;

    public class Probe {
	const uint PREFMAXLEN = 0xFFFFFFFF;

	const int ACCESS_READ	= 0x01;
	const int ACCESS_WRITE	= 0x02;
	const int ACCESS_CREATE	= 0x04;
	const int ACCESS_EXEC	= 0x08;
	const int ACCESS_DELETE	= 0x10;
	const int ACCESS_ATRIB	= 0x20;
	const int ACCESS_PERM	= 0x40;

	public enum SHARE_TYPE : uint {
	    STYPE_DISKTREE	= 0,
	    STYPE_PRINTQ	= 1,
	    STYPE_DEVICE	= 2,
	    STYPE_IPC		= 3,
	    STYPE_SPECIAL	= 0x80000000
	}

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct SHARE_INFO_2 {
            [MarshalAs(UnmanagedType.LPWStr)]
            public string shi2_netname;
            public uint shi2_type;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string shi2_remark;
            public uint shi2_permissions;
            public uint shi2_max_uses;
            public uint shi2_current_uses;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string shi2_path;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string shi2_passwd;
        }

	[DllImport("Netapi32.dll", SetLastError = true)]
	static extern int NetApiBufferFree(IntPtr Buffer);

	[DllImport("Netapi32.dll", CharSet = CharSet.Unicode)]
	static extern int NetShareEnum(StringBuilder ServerName, int level, ref IntPtr bufptr, uint prefmaxlen,
				       ref int entriesread, ref int totalentries, ref int resume_handle);

	public static List<SHARE_INFO_2> List() {
	    int entriesread = 0;
	    int totalentries = 0;
	    int resume_handle = 0;
	    int nStructSize = Marshal.SizeOf(typeof(SHARE_INFO_2));
	    IntPtr bufptr = IntPtr.Zero;
	    int result = NetShareEnum(null, 2, ref bufptr, PREFMAXLEN, ref entriesread, ref totalentries, ref resume_handle);
	    if (result == 0) {
		List<SHARE_INFO_2> ShareInfos = new List<SHARE_INFO_2>();
	        IntPtr shi2ptr = bufptr;
	        for (int i = 0; i < entriesread; i++) {
		    SHARE_INFO_2 shi2 = (SHARE_INFO_2)Marshal.PtrToStructure(shi2ptr, typeof(SHARE_INFO_2));
		    ShareInfos.Add(shi2);
		    shi2ptr = new IntPtr(shi2ptr.ToInt32() + nStructSize);
	        }
	        NetApiBufferFree(bufptr);
	        return ShareInfos;
	    } else {
		throw new System.ComponentModel.Win32Exception(result);
	    }
	}
    }
}
