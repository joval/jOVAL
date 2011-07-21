// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

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

    static final String IP4_ADDRESS		= "ipv4_address";
    static final String IP6_ADDRESS		= "ipv6_address";

    static final String COMPUTERNAME_KEY	= "System\\CurrentControlSet\\Control\\ComputerName\\ComputerName";
    static final String COMPUTERNAME_VAL	= "ComputerName";
    static final String CURRENTVERSION_KEY	= "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
    static final String CURRENTVERSION_VAL	= "CurrentVersion";
    static final String PRODUCTNAME_VAL		= "ProductName";

    static final String CIMV2NS			= "root\\cimv2";
    static final String ADAPTER_WQL		= "select * from Win32_NetworkAdapterConfiguration";
    static final String MAC_ADDR_FIELD		= "MACAddress";
    static final String IP_ADDR_FIELD		= "IPAddress";
    static final String DESCRIPTION_FIELD	= "Description";

    private IRegistry registry;
    private IWmiProvider wmi;
    private ObjectFactory coreFactory;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public WindowsSystemInfo(IRegistry registry, IWmiProvider wmi) {
	coreFactory = new ObjectFactory();
	this.registry = registry;
	this.wmi = wmi;
    }

    public SystemInfoType getSystemInfo() throws Exception {
	if (info != null) {
	    return info;
	}

	info = coreFactory.createSystemInfoType();
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
		throw new Exception(JOVALSystem.getMessage("ERROR_WINREG_CONNECT"));
	    }

	    wmiConnected = wmi.connect();
	    if (wmiConnected) {
		InterfacesType interfacesType = coreFactory.createInterfacesType();
		ISWbemObjectSet result = wmi.execQuery(CIMV2NS, ADAPTER_WQL);
		Iterator <ISWbemObject>iter = result.iterator();
		while (iter.hasNext()) {
		    ISWbemPropertySet row = iter.next().getProperties();
		    String macAddress = row.getItem(MAC_ADDR_FIELD).getValueAsString();
		    if (macAddress != null) {
			String[] ipAddresses = row.getItem(IP_ADDR_FIELD).getValueAsArray();
			if (ipAddresses != null && ipAddresses.length > 0) {
			    for (int i=0; i < 2 && i < ipAddresses.length; i++) {
				InterfaceType interfaceType = coreFactory.createInterfaceType();
				interfaceType.setMacAddress(macAddress);
				String description = row.getItem(DESCRIPTION_FIELD).getValueAsString();
				if (description != null) {
				    interfaceType.setInterfaceName(description);
				}
				EntityItemIPAddressStringType ipAddressType = coreFactory.createEntityItemIPAddressStringType();
				ipAddressType.setValue(ipAddresses[i]);
				switch(i) {
				  case 0: // The IPv4 address is [0]
				    ipAddressType.setDatatype(IP4_ADDRESS);
				    break;
				  case 1: // The IPv6 address is [1]
				    ipAddressType.setDatatype(IP6_ADDRESS);
				    break;
				}
				interfaceType.setIpAddress(ipAddressType);
				interfacesType.getInterface().add(interfaceType);
			    }
			}
		    }
		}
		info.setInterfaces(interfacesType);
	    } else {
		throw new Exception(JOVALSystem.getMessage("ERROR_WMI_CONNECT"));
	    }
	} catch (WmiException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PLUGIN_INTERFACE"), e);
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
