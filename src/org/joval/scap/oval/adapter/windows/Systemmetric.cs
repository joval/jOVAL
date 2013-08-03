// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace SystemMetrics {
	public enum SystemMetric:int {
	    SM_CXSCREEN				= 0x00,
	    SM_CYSCREEN				= 0x01,
	    SM_CXVSCROLL			= 0x02,
	    SM_CYHSCROLL			= 0x03,
	    SM_CYCAPTION			= 0x04,
	    SM_CXBORDER				= 0x05,
	    SM_CYBORDER				= 0x06,
	    SM_CXDLGFRAME			= 0x07,
	    SM_CXFIXEDFRAME			= 0x07,
	    SM_CYDLGFRAME			= 0x08,
	    SM_CYFIXEDFRAME			= 0x08,
	    SM_CYVTHUMB				= 0x09,
	    SM_CXHTHUMB				= 0x0A,
	    SM_CXICON				= 0x0B,
	    SM_CYICON				= 0x0C,
	    SM_CXCURSOR				= 0x0D,
	    SM_CYCURSOR				= 0x0E,
	    SM_CYMENU				= 0x0F,
	    SM_CXFULLSCREEN			= 0x10,
	    SM_CYFULLSCREEN			= 0x11,
	    SM_CYKANJIWINDOW			= 0x12,
	    SM_MOUSEPRESENT			= 0x13,
	    SM_CYVSCROLL			= 0x14,
	    SM_CXHSCROLL			= 0x15,
	    SM_DEBUG				= 0x16,
	    SM_SWAPBUTTON			= 0x17,
	    SM_CXMIN				= 0x1C,
	    SM_CYMIN				= 0x1D,
	    SM_CXSIZE				= 0x1E,
	    SM_CYSIZE				= 0x1F,
	    SM_CXSIZEFRAME			= 0x20,
	    SM_CXFRAME				= 0x20,
	    SM_CYSIZEFRAME			= 0x21,
	    SM_CYFRAME				= 0x21,
	    SM_CXMINTRACK			= 0x22,
	    SM_CYMINTRACK			= 0x23,
	    SM_CXDOUBLECLK			= 0x24,
	    SM_CYDOUBLECLK			= 0x25,
	    SM_CXICONSPACING			= 0x26,
	    SM_CYICONSPACING			= 0x27,
	    SM_MENUDROPALIGNMENT		= 0x28,
	    SM_PENWINDOWS			= 0x29,
	    SM_DBCSENABLED			= 0x2A,
	    SM_CMOUSEBUTTONS			= 0x2B,
	    SM_SECURE				= 0x2C,
	    SM_CXEDGE				= 0x2D,
	    SM_CYEDGE				= 0x2E,
	    SM_CXMINSPACING			= 0x2F,
	    SM_CYMINSPACING			= 0x30,
	    SM_CXSMICON				= 0x31,
	    SM_CYSMICON				= 0x32,
	    SM_CYSMCAPTION			= 0x33,
	    SM_CXSMSIZE				= 0x34,
	    SM_CYSMSIZE				= 0x35,
	    SM_CXMENUSIZE			= 0x36,
	    SM_CYMENUSIZE			= 0x37,
	    SM_ARRANGE				= 0x38,
	    SM_CXMINIMIZED			= 0x39,
	    SM_CYMINIMIZED			= 0x3A,
	    SM_CXMAXTRACK			= 0x3B,
	    SM_CYMAXTRACK			= 0x3C,
	    SM_CXMAXIMIZED			= 0x3D,
	    SM_CYMAXIMIZED			= 0x3E,
	    SM_NETWORK				= 0x3F,
	    SM_CLEANBOOT			= 0x43,
	    SM_CXDRAG				= 0x44,
	    SM_CYDRAG				= 0x45,
	    SM_SHOWSOUNDS			= 0x46,
	    SM_CXMENUCHECK			= 0x47,
	    SM_CYMENUCHECK			= 0x48,
	    SM_SLOWMACHINE			= 0x49,
	    SM_MIDEASTENABLED			= 0x4A,
	    SM_MOUSEWHEELPRESENT		= 0x4B,
	    SM_XVIRTUALSCREEN			= 0x4C,
	    SM_YVIRTUALSCREEN			= 0x4D,
	    SM_CXVIRTUALSCREEN			= 0x4E,
	    SM_CYVIRTUALSCREEN			= 0x4F,
	    SM_CMONITORS			= 0x50,
	    SM_SAMEDISPLAYFORMAT		= 0x51,
	    SM_IMMENABLED			= 0x52,
	    SM_CXFOCUSBORDER			= 0x53,
	    SM_CYFOCUSBORDER			= 0x54,
	    SM_TABLETPC				= 0x56,
	    SM_MEDIACENTER			= 0x57,
	    SM_STARTER				= 0x58,
	    SM_SERVERR2				= 0x59,
	    SM_MOUSEHORIZONTALWHEELPRESENT	= 0x5B,
	    SM_CXPADDEDBORDER			= 0x5C,
	    SM_DIGITIZER			= 0x5E,
	    SM_MAXIMUMTOUCHES			= 0x5F,
	    SM_REMOTESESSION			= 0x1000,
	    SM_SHUTTINGDOWN			= 0x2000,
	    SM_REMOTECONTROL			= 0x2001
	}

	public class Probe {
	    [DllImport("user32.dll")]
	    static extern int GetSystemMetrics(SystemMetric smIndex);

	    public static List<String> getMetrics() {
		List<String> retList = new List<String>();
		SystemMetric[] metrics = (SystemMetric[])Enum.GetValues(typeof(SystemMetric));
		for (int i=0; i < metrics.Length; i++) {
		    retList.Add(String.Format("{0}: {1:D}", metrics[i].ToString(), GetSystemMetrics(metrics[i])));
		}
		return retList;
	    }
	}
    }
}
