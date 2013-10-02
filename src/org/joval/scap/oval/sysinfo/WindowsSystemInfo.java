// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import jsaf.intf.system.IEnvironment;
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.registry.IStringValue;
import jsaf.intf.windows.registry.IValue;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemObjectSet;
import jsaf.intf.windows.wmi.ISWbemProperty;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.InterfacesType;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.SystemInfoType;

import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Tool for creating a SystemInfoType from an IRegistry and an IWmiProvider.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class WindowsSystemInfo {
    public static final String ARCHITECTURE	= "PROCESSOR_ARCHITECTURE";

    static final String ENVIRONMENT_KEY		= "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
    static final String ARCHITECTURE_VAL	= "PROCESSOR_ARCHITECTURE";
    static final String CURRENTVERSION_KEY	= "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
    static final String CURRENTVERSION_VAL	= "CurrentVersion";
    static final String PRODUCTNAME_VAL		= "ProductName";

    public static SystemInfoType getSystemInfo(IWindowsSession session) {
	IRegistry registry = session.getRegistry(session.getNativeView());
	IWmiProvider wmi = session.getWmiProvider();
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setPrimaryHostName(session.getMachineName());

	try {
	    info.setArchitecture(registry.getStringValue(IRegistry.Hive.HKLM, ENVIRONMENT_KEY, ARCHITECTURE_VAL));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_ARCH);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    info.setOsVersion(registry.getStringValue(IRegistry.Hive.HKLM, CURRENTVERSION_KEY, CURRENTVERSION_VAL));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSVERSION);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    info.setOsName(registry.getStringValue(IRegistry.Hive.HKLM, CURRENTVERSION_KEY, PRODUCTNAME_VAL));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSNAME);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    InterfacesType interfacesType = Factories.sc.core.createInterfacesType();
	    for (NetworkInterface intf : NetworkInterface.getInterfaces(session)) {
		if (intf == null) {
		    continue;
		} else if (intf.getIPAddresses() != null) {
		    for (NetworkInterface.IPAddress addr : intf.getIPAddresses()) {
			InterfaceType interfaceType = Factories.sc.core.createInterfaceType();
			interfaceType.setMacAddress(intf.getHardwareAddress());
			interfaceType.setInterfaceName(intf.getName());
			interfaceType.setIpAddress(addr.getAddress());
			interfacesType.getInterface().add(interfaceType);
		    }
		}
	    }
	    info.setInterfaces(interfacesType);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_INTERFACE);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return info;
    }
}
