// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace LockoutPolicy {
	[StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
	struct USER_MODALS_INFO_0 {
	    public Int32 usrmod0_min_passwd_len;
	    public Int32 usrmod0_max_passwd_age;
	    public Int32 usrmod0_min_passwd_age;
	    public Int32 usrmod0_force_logoff;
	    public Int32 usrmod0_password_hist_len;
	}

	[StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
	struct USER_MODALS_INFO_3 {
	    public Int32 usrmod3_lockout_duration;
	    public Int32 usrmod3_lockout_observation_window;
	    public Int32 usrmod3_lockout_threshold;
	}

	public class Probe {
	    [DllImport("netapi32.dll", CharSet=CharSet.Unicode, CallingConvention=CallingConvention.StdCall, SetLastError=true)]
	    static extern uint NetUserModalsGet(String server, int level, out IntPtr BufPtr);

	    [DllImport("netapi32.dll", SetLastError=true)]
	    static extern int NetApiBufferFree(IntPtr lpBuffer);

	    public static List<String> getLockoutPolicy() {
		List<String> retList = new List<String>();
		USER_MODALS_INFO_0 info0 = new USER_MODALS_INFO_0();
		USER_MODALS_INFO_3 info3 = new USER_MODALS_INFO_3();
		IntPtr ptr;
		uint code = NetUserModalsGet(null, 0, out ptr);
		if (code == 0) {
		    info0 = (USER_MODALS_INFO_0)Marshal.PtrToStructure(ptr, typeof(USER_MODALS_INFO_0));
		    retList.Add(String.Format("force_logoff={0:X8}", info0.usrmod0_force_logoff));
		    NetApiBufferFree(ptr);
		    ptr = IntPtr.Zero;
		}
		code = NetUserModalsGet(null, 3, out ptr);
		if (code == 0) {
		    info3 = (USER_MODALS_INFO_3)Marshal.PtrToStructure(ptr, typeof(USER_MODALS_INFO_3));
		    retList.Add(String.Format("lockout_duration={0:X8}", info3.usrmod3_lockout_duration));
		    retList.Add(String.Format("lockout_observation_window={0:X8}", info3.usrmod3_lockout_observation_window));
		    retList.Add(String.Format("lockout_threshold={0:X8}", info3.usrmod3_lockout_threshold));
		    NetApiBufferFree(ptr);
		    ptr = IntPtr.Zero;
		}
		return retList;
	    }
	}
    }
}
