// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.sysinfo;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Something that requires a credential for access.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SysinfoFactory {
    /**
     * Map the IBaseSession to the correct FamilyEnumeration.
     */
    public static FamilyEnumeration getFamily(IBaseSession session) {
	switch(session.getType()) {
	  case WINDOWS:
	    return FamilyEnumeration.WINDOWS;

	  case UNIX:
	    switch(((IUnixSession)session).getFlavor()) {
	      case MACOSX:
		return FamilyEnumeration.MACOS;
	      default:
		return FamilyEnumeration.UNIX;
	    }
	
	  case CISCO_IOS:
	    return FamilyEnumeration.IOS;

	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }

    /**
     * Create OVAL system information from the supplied session.
     */
    public static SystemInfoType createSystemInfo(IBaseSession session) throws OvalException {
	switch(session.getType()) {
	  case CISCO_IOS:
	    return IosSystemInfo.getSystemInfo((IIosSession)session);

	  case JUNIPER_JUNOS:
	    return JunosSystemInfo.getSystemInfo((IJunosSession)session);

	  case WINDOWS:
	    return WindowsSystemInfo.getSystemInfo((IWindowsSession)session);

	  case UNIX:
	    return UnixSystemInfo.getSystemInfo((IUnixSession)session);

	  default:
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SYSINFO_TYPE, session.getClass().getName()));
	}
    }
}
