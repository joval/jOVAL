// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
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

    static final String CURRENTVERSION_KEY	= "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
    static final String CURRENTVERSION_VAL	= "CurrentVersion";
    static final String PRODUCTNAME_VAL		= "ProductName";

    static final String MAC_ADDR_FIELD		= "MACAddress";
    static final String IP_ADDR_FIELD		= "IPAddress";
    static final String DESCRIPTION_FIELD	= "Description";
    static final String ADAPTER_WQL		= "select * from Win32_NetworkAdapterConfiguration";

    public static SystemInfoType getSystemInfo(IWindowsSession session) {
	IRegistry registry = session.getRegistry(IWindowsSession.View._64BIT);
	IWmiProvider wmi = session.getWmiProvider();
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setPrimaryHostName(session.getHostname());

	try {
	    IEnvironment environment = registry.getEnvironment();
	    info.setArchitecture(environment.getenv(ARCHITECTURE));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_ARCH);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    IKey key = registry.fetchKey(IRegistry.HKLM, CURRENTVERSION_KEY);
	    IValue val = key.getValue(CURRENTVERSION_VAL);
	    if (val.getType() == IValue.REG_SZ) {
		info.setOsVersion(((IStringValue)val).getData());
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSVERSION);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    IKey key = registry.fetchKey(IRegistry.HKLM, CURRENTVERSION_KEY);
	    IValue val = key.getValue(PRODUCTNAME_VAL);
	    if (val.getType() == IValue.REG_SZ) {
		info.setOsName(((IStringValue)val).getData());
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSNAME);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    InterfacesType interfacesType = Factories.sc.core.createInterfacesType();
	    ISWbemObjectSet result = wmi.execQuery(IWmiProvider.CIMv2, ADAPTER_WQL);
	    Iterator <ISWbemObject>iter = result.iterator();
	    while (iter.hasNext()) {
		ISWbemPropertySet row = iter.next().getProperties();
		ISWbemProperty macAddress = row.getItem(MAC_ADDR_FIELD);
		if (macAddress != null) {
		    ISWbemProperty ipAddress = row.getItem(IP_ADDR_FIELD);
		    if (ipAddress != null) {
			String[] ipAddresses = ipAddress.getValueAsArray();
			if (ipAddresses != null) {
			    for (int i=0; i < 2 && i < ipAddresses.length; i++) {
				InterfaceType interfaceType = Factories.sc.core.createInterfaceType();
				interfaceType.setMacAddress(macAddress.getValueAsString());
				String description = row.getItem(DESCRIPTION_FIELD).getValueAsString();
				if (description != null) {
				    interfaceType.setInterfaceName(description);
				}
				interfaceType.setIpAddress(ipAddresses[i]);
				interfacesType.getInterface().add(interfaceType);
			    }
			}
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
