// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.sysinfo;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.apple.system.IiOSSession;
import org.joval.oval.Factories;

/**
 * Tool for creating a SystemInfoType from an offline Apple IiOSSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class AppleiOSSystemInfo {
    static SystemInfoType getSystemInfo(IiOSSession session) {
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setOsName("Apple iOS");
	info.setOsVersion("unknown");
	info.setArchitecture("unknown");
	info.setPrimaryHostName(session.getHostname());

	info.setInterfaces(Factories.sc.core.createInterfacesType());
	return info;
    }
}
