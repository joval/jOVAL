// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace Process58 {
	public class Probe {
	    public const int PROCESS_DEP_DISABLE = 0;
	    public const int PROCESS_DEP_ENABLE = 1;

	    [Flags]
	    public enum ProcessAccessFlags : uint {
		All			= 0x001F0FFF,
		Terminate		= 0x00000001,
		CreateThread		= 0x00000002,
		VMOperation		= 0x00000008,
		VMRead			= 0x00000010,
		VMWrite			= 0x00000020,
		DupHandle		= 0x00000040,
		SetInformation		= 0x00000200,
		QueryInformation	= 0x00000400,
		Synchronize		= 0x00100000
	    }

	    [DllImport("kernel32.dll")]
	    public static extern int GetLastError();

	    [DllImport("kernel32.dll", SetLastError=true)][return: MarshalAs(UnmanagedType.Bool)]
	    public static extern bool CloseHandle(IntPtr hObject);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    static extern bool GetProcessDEPPolicy(IntPtr hProcess, out UInt32 Flags, out bool Permanent);

	    [DllImport("user32.dll", SetLastError=true, CharSet=CharSet.Auto)]
	    static extern int GetWindowTextLength(IntPtr hWnd);

	    [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
	    static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

	    [DllImport("kernel32.dll")]
	    public static extern IntPtr OpenProcess(ProcessAccessFlags dwDesiredAccess, bool bInheritHandle, int dwProcessId);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    public static extern bool IsWow64Process(IntPtr hProcess, out bool wow64Process);

	    /**
	     * Returns 1 if enabled, 0 if disabled, -1 if not applicable (i.e., a 64-bit process)
	     */
	    public static int IsDepEnabled(UInt32 pid) {
		UInt32 Flags = PROCESS_DEP_DISABLE;
		bool Permanent = false;

		IntPtr hProcess = IntPtr.Zero;
		hProcess = OpenProcess(ProcessAccessFlags.QueryInformation, false, (int)pid);
		if (hProcess == IntPtr.Zero) {
		    throw new System.ComponentModel.Win32Exception(GetLastError());
		}

		bool is32bit = false;
		if (!IsWow64Process(hProcess, out is32bit)) {
		    throw new System.ComponentModel.Win32Exception(GetLastError());
		}
		if (is32bit) {
		    if (GetProcessDEPPolicy(hProcess, out Flags, out Permanent)) {
			CloseHandle(hProcess);
			if ((Flags | PROCESS_DEP_ENABLE) == PROCESS_DEP_ENABLE) {
			    return 1;
			} else {
			    return 0;
			}
		    } else {
			CloseHandle(hProcess);
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		} else {
		    return -1;
		}
	    }

	    public static String GetWindowText(UInt32 pid) {
		Process p = Process.GetProcessById((int)pid);
		if (p != null) {
		    IntPtr hWnd = p.MainWindowHandle;
		    if (hWnd != IntPtr.Zero) {
			int len = GetWindowTextLength(hWnd);
			StringBuilder sb = new StringBuilder(len + 1);
			GetWindowText(hWnd, sb, sb.Capacity);
			return sb.ToString();
		    }
		}
		return null;
	    }
	}
    }
}
