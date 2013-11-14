// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.system.IWindowsSession;

import scap.oval.common.FamilyEnumeration;
import scap.oval.systemcharacteristics.core.SystemInfoType;

import org.joval.scap.oval.Factories;

/**
 * Something that requires a credential for access.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SysinfoFactory {
    /**
     * Map the ISession to the correct FamilyEnumeration.
     */
    public static FamilyEnumeration getFamily(ISession session) {
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
	
	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }

    /**
     * Create OVAL system information from the supplied session.
     */
    public static SystemInfoType createSystemInfo(ISession session) {
	switch(session.getType()) {
	  case WINDOWS:
	    return WindowsSystemInfo.getSystemInfo((IWindowsSession)session);

	  case UNIX:
	    return UnixSystemInfo.getSystemInfo((IUnixSession)session);

	  default:
	    SystemInfoType info = Factories.sc.core.createSystemInfoType();
	    info.setPrimaryHostName(session.getHostname());
	    info.setArchitecture("unknown");
	    info.setOsName("unknown");
	    info.setOsVersion("unknown");
	    info.getInterfaces(); // empty
	    return info;
	}
    }
}
