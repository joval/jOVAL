// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL.Printer {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.ServiceProcess;
    using System.Text;

    public class Probe {
	[FlagsAttribute]
	enum PrinterEnumFlags {
	    PRINTER_ENUM_DEFAULT	= 0x00000001,
	    PRINTER_ENUM_LOCAL		= 0x00000002,
	    PRINTER_ENUM_CONNECTIONS	= 0x00000004,
	    PRINTER_ENUM_FAVORITE	= 0x00000004,
	    PRINTER_ENUM_NAME		= 0x00000008,
	    PRINTER_ENUM_REMOTE		= 0x00000010,
	    PRINTER_ENUM_SHARED		= 0x00000020,
	    PRINTER_ENUM_NETWORK	= 0x00000040,
	    PRINTER_ENUM_EXPAND		= 0x00004000,
	    PRINTER_ENUM_CONTAINER	= 0x00008000,
	    PRINTER_ENUM_ICONMASK	= 0x00ff0000,
	    PRINTER_ENUM_ICON1		= 0x00010000,
	    PRINTER_ENUM_ICON2		= 0x00020000,
	    PRINTER_ENUM_ICON3		= 0x00040000,
	    PRINTER_ENUM_ICON4		= 0x00080000,
	    PRINTER_ENUM_ICON5		= 0x00100000,
	    PRINTER_ENUM_ICON6		= 0x00200000,
	    PRINTER_ENUM_ICON7		= 0x00400000,
	    PRINTER_ENUM_ICON8		= 0x00800000,
	    PRINTER_ENUM_HIDE		= 0x01000000
	}

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
	public struct PRINTER_INFO_2 {
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pServerName;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pPrinterName;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pShareName;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pPortName;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pDriverName;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pComment;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pLocation;
	    public IntPtr pDevMode;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pSepFile;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pPrintProcessor;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pDatatype;
	    [MarshalAs(UnmanagedType.LPTStr)]
	    public string pParameters;
	    public IntPtr pSecurityDescriptor;
	    public uint Attributes; // See note below!
	    public uint Priority;
	    public uint DefaultPriority;
	    public uint StartTime;
	    public uint UntilTime;
	    public uint Status;
	    public uint cJobs;
	    public uint AveragePPM;
	}

	[DllImport("winspool.drv", CharSet = CharSet.Auto, SetLastError = true)]
	static extern bool EnumPrinters(PrinterEnumFlags Flags, string Name, uint Level, IntPtr pPrinterEnum,
					uint cbBuf, ref uint pcbNeeded, ref uint pcReturned);

	private const int ERROR_INSUFFICIENT_BUFFER = 122;

	public static List<PRINTER_INFO_2> List() {
	    uint cbNeeded = 0;
	    uint cReturned = 0;
	    PrinterEnumFlags flags = PrinterEnumFlags.PRINTER_ENUM_LOCAL;
	    if (EnumPrinters(flags, null, 2, IntPtr.Zero, 0, ref cbNeeded, ref cReturned)) {
		return null;
	    }
	    int lastWin32Error = Marshal.GetLastWin32Error();
	    if (lastWin32Error == ERROR_INSUFFICIENT_BUFFER) {
		IntPtr pAddr = Marshal.AllocHGlobal((int)cbNeeded);
		if (EnumPrinters(flags, null, 2, pAddr, cbNeeded, ref cbNeeded, ref cReturned)) {
		    List<PRINTER_INFO_2> PrinterInfos = new List<PRINTER_INFO_2>();
		    int offset = pAddr.ToInt32();
		    Type type = typeof(PRINTER_INFO_2);
		    int increment = Marshal.SizeOf(type);
		    for (int i=0; i < cReturned; i++) {
			PRINTER_INFO_2 pi2 = (PRINTER_INFO_2)Marshal.PtrToStructure(new IntPtr(offset), type);
			PrinterInfos.Add(pi2);
			offset += increment;
		    }
		    Marshal.FreeHGlobal(pAddr);
		    return PrinterInfos;
		}
		lastWin32Error = Marshal.GetLastWin32Error();
	    }
	    throw new System.ComponentModel.Win32Exception(lastWin32Error);
	}
    }
}
