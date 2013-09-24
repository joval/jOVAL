// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.ServiceProcess;
    using System.Text;

    namespace Service {
	public enum SERVICE_CONTROL : uint {
            STOP			= 0x00000001,
            PAUSE			= 0x00000002,
            CONTINUE			= 0x00000003,
            INTERROGATE			= 0x00000004,
            SHUTDOWN			= 0x00000005,
            PARAMCHANGE			= 0x00000006,
            NETBINDADD			= 0x00000007,
            NETBINDREMOVE		= 0x00000008,
            NETBINDENABLE		= 0x00000009,
            NETBINDDISABLE		= 0x0000000A,
            DEVICEEVENT			= 0x0000000B,
            HARDWAREPROFILECHANGE	= 0x0000000C,
            POWEREVENT			= 0x0000000D,
            SESSIONCHANGE		= 0x0000000E
	}

	public enum SERVICE_STATE : uint {
            SERVICE_STOPPED		= 0x00000001,
            SERVICE_START_PENDING	= 0x00000002,
            SERVICE_STOP_PENDING	= 0x00000003,
            SERVICE_RUNNING		= 0x00000004,
            SERVICE_CONTINUE_PENDING	= 0x00000005,
            SERVICE_PAUSE_PENDING	= 0x00000006,
            SERVICE_PAUSED		= 0x00000007
	}

	[Flags]
	public enum SERVICE_ACCEPT : uint {
            STOP			= 0x00000001,
            PAUSE_CONTINUE		= 0x00000002,
            SHUTDOWN			= 0x00000004,
            PARAMCHANGE			= 0x00000008,
            NETBINDCHANGE		= 0x00000010,
            HARDWAREPROFILECHANGE	= 0x00000020,
            POWEREVENT			= 0x00000040,
            SESSIONCHANGE		= 0x00000080
	}

	[StructLayout(LayoutKind.Sequential)]
	public struct SERVICE_STATUS {
            public int serviceType;
            public int currentState;
            public int controlsAccepted;
            public int win32ExitCode;
            public int serviceSpecificExitCode;
            public int checkPoint;
            public int waitHint;
	}

	public class Probe {
	    [DllImport("kernel32.dll")]
	    internal static extern int GetLastError();

	    [DllImport ("advapi32.dll", EntryPoint = "QueryServiceStatus", CharSet = CharSet.Auto)]
	    internal static extern bool QueryServiceStatus(SafeHandle hService, ref SERVICE_STATUS dwServiceStatus);

	    public static int GetAcceptFlags(String name) {
		ServiceController sc = new ServiceController(name);
		SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
		if (QueryServiceStatus(sc.ServiceHandle, ref serviceStatus)) {
		    return serviceStatus.controlsAccepted;
		} else {
		    throw new System.ComponentModel.Win32Exception(GetLastError());
		}
	    }
	}
    }
}
