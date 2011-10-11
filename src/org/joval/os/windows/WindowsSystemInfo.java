// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows;

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
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from an IRegistry and an IWmiProvider.  This will cause the IRegistry and IWmiProvider
 * to be connected and disconnected.  Hence, after fetching the SystemInfoType, it is safe to get the IEnvironment from the
 * IRegistry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSystemInfo {
    public static final String ARCHITECTURE	= "PROCESSOR_ARCHITECTURE";

    static final String COMPUTERNAME_KEY	= "System\\CurrentControlSet\\Control\\ComputerName\\ComputerName";
    static final String COMPUTERNAME_VAL	= "ComputerName";
    static final String CURRENTVERSION_KEY	= "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
    static final String CURRENTVERSION_VAL	= "CurrentVersion";
    static final String PRODUCTNAME_VAL		= "ProductName";

    static final String ADAPTER_WQL		= "select * from Win32_NetworkAdapterConfiguration";
    static final String MAC_ADDR_FIELD		= "MACAddress";
    static final String IP_ADDR_FIELD		= "IPAddress";
    static final String DESCRIPTION_FIELD	= "Description";

    private IWindowsSession session;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public WindowsSystemInfo(IWindowsSession session) {
	this.session = session;
    }

    public SystemInfoType getSystemInfo() {
	if (info != null) {
	    return info;
	}

	IRegistry registry = session.getRegistry(IWindowsSession.View._64BIT);
	IWmiProvider wmi = session.getWmiProvider();
	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	boolean regConnected=false, wmiConnected=false;
	try {
	    regConnected = registry.connect();
	    if (regConnected) {
		IEnvironment environment = registry.getEnvironment();
		info.setArchitecture(environment.getenv(ARCHITECTURE));

		IKey cn = registry.fetchKey(IRegistry.HKLM, COMPUTERNAME_KEY);
		IValue cnVal = cn.getValue(COMPUTERNAME_VAL);
		if (cnVal.getType() == IValue.REG_SZ) {
		    info.setPrimaryHostName(((IStringValue)cnVal).getData());
		}
		IKey cv = registry.fetchKey(IRegistry.HKLM, CURRENTVERSION_KEY);
		IValue cvVal = cv.getValue(CURRENTVERSION_VAL);
		if (cvVal.getType() == IValue.REG_SZ) {
		    info.setOsVersion(((IStringValue)cvVal).getData());
		}
		IValue pnVal = cv.getValue(PRODUCTNAME_VAL);
		if (pnVal.getType() == IValue.REG_SZ) {
		    info.setOsName(((IStringValue)pnVal).getData());
		}
	    } else {
		throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_CONNECT));
	    }

	    wmiConnected = wmi.connect();
	    if (wmiConnected) {
		InterfacesType interfacesType = JOVALSystem.factories.sc.core.createInterfacesType();
		ISWbemObjectSet result = wmi.execQuery(IWmiProvider.CIMv2, ADAPTER_WQL);
		Iterator <ISWbemObject>iter = result.iterator();
		while (iter.hasNext()) {
		    ISWbemPropertySet row = iter.next().getProperties();
		    String macAddress = row.getItem(MAC_ADDR_FIELD).getValueAsString();
		    if (macAddress != null) {
			String[] ipAddresses = row.getItem(IP_ADDR_FIELD).getValueAsArray();
			if (ipAddresses != null && ipAddresses.length > 0) {
			    for (int i=0; i < 2 && i < ipAddresses.length; i++) {
				InterfaceType interfaceType = JOVALSystem.factories.sc.core.createInterfaceType();
				interfaceType.setMacAddress(macAddress);
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
		info.setInterfaces(interfacesType);
	    } else {
		throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_WMI_CONNECT));
	    }
	} catch (WmiException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_INTERFACE);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    try {
		if (regConnected) {
		    registry.disconnect();
		}
	    } finally {
		if (wmiConnected) {
		    wmi.disconnect();
		}
	    }
	}
	return info;
    }
}
