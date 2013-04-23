# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-LockoutPolicy {
  $Source = @"
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace jOVAL.LockoutPolicy {
      public class Probe {
	[DllImport("netapi32.dll", CharSet=CharSet.Unicode, CallingConvention=CallingConvention.StdCall, SetLastError=true)]
	static extern uint NetUserModalsGet(string server, int level, out IntPtr BufPtr);

	[DllImport("netapi32.dll", SetLastError=true)]
	static extern int NetApiBufferFree(IntPtr lpBuffer);

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

	public static List<string> getLockoutPolicy() {
	  List<string> retList = new List<string>();
	  USER_MODALS_INFO_0 info0 = new USER_MODALS_INFO_0();
	  USER_MODALS_INFO_3 info3 = new USER_MODALS_INFO_3();
	  IntPtr ptr;
	  uint code = NetUserModalsGet(null, 0, out ptr);
	  if (code == 0) {
	    info0 = (USER_MODALS_INFO_0)Marshal.PtrToStructure(ptr, typeof(USER_MODALS_INFO_0));
	    retList.Add("force_logoff=" + info0.usrmod0_force_logoff);
	    NetApiBufferFree(ptr);
	    ptr = IntPtr.Zero;
	  }
	  code = NetUserModalsGet(null, 3, out ptr);
	  if (code == 0) {
	    info3 = (USER_MODALS_INFO_3)Marshal.PtrToStructure(ptr, typeof(USER_MODALS_INFO_3));
	    retList.Add("lockout_duration=" + info3.usrmod3_lockout_duration);
	    retList.Add("lockout_observation_window=" + info3.usrmod3_lockout_observation_window);
	    retList.Add("lockout_threshold=" + info3.usrmod3_lockout_threshold);
	    NetApiBufferFree(ptr);
	    ptr = IntPtr.Zero;
	  }
	  return retList;
	}
      }
    }
"@

  $ErrorActionPreference = "SilentlyContinue"
  $type = [jOVAL.LockoutPolicy.Probe]

  $ErrorActionPreference = "Stop"
  if ($type -eq $null) {
    New-Type -TypeDefinition $Source
  }

  $ErrorActionPreference = "Continue"
  $result = [jOVAL.LockoutPolicy.Probe]::getLockoutPolicy()
  foreach($token in $result) {
    Write-Output $token
  }
}
