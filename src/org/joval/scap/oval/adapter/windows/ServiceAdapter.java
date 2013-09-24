// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.ServiceObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemServiceControlsAcceptedType;
import scap.oval.systemcharacteristics.windows.EntityItemServiceCurrentStateType;
import scap.oval.systemcharacteristics.windows.EntityItemServiceStartTypeType;
import scap.oval.systemcharacteristics.windows.EntityItemServiceTypeType;
import scap.oval.systemcharacteristics.windows.ServiceItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Service objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ServiceAdapter extends BaseServiceAdapter<ServiceItem> {
    private Map<String, ServiceItem> services;

    // Protected

    protected Class getObjectClass() {
        return ServiceObject.class;
    }

    protected Class getItemClass() {
        return ServiceItem.class;
    }

    protected Collection<ServiceItem> getItems(ObjectType obj, ItemType base, IRequestContext rc) throws CollectException {
	init();
	ServiceItem item = (ServiceItem)base;
	String name = (String)item.getServiceName().getValue();
	if (services.containsKey(name.toLowerCase())) {
	    ServiceItem temp = services.get(name.toLowerCase());
	    item.getControlsAccepted().addAll(temp.getControlsAccepted());
	    item.setCurrentState(temp.getCurrentState());
	    item.getDependencies().addAll(temp.getDependencies());
	    item.setDescription(temp.getDescription());
	    item.setDisplayName(temp.getDisplayName());
	    item.setPath(temp.getPath());
	    item.setPid(temp.getPid());
	    item.setServiceFlag(temp.getServiceFlag());
	    item.getServiceType().addAll(temp.getServiceType());
	    item.setStartName(temp.getStartName());
	    item.setStartType(temp.getStartType());
	} else {
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	return Arrays.asList(item);
    }

    @Override
    protected List<InputStream> getPowershellAssemblies() {
        return Arrays.asList(getClass().getResourceAsStream("Service.dll"));
    }

    @Override
    protected List<InputStream> getPowershellModules() {
        return Arrays.asList(getClass().getResourceAsStream("Service.psm1"));
    }

    // Private

    /**
     * Idempotent
     */
    private void init() throws CollectException {
	if (services == null) {
	    services = new HashMap<String, ServiceItem>();
	    try {
		initInternal();
	    } catch (Exception e) {
		throw new CollectException(e, FlagEnumeration.ERROR);
	    }
	}
    }

    /**
     * NOT idempotent. Creates a hashtable of items about every service.
     */
    private void initInternal() throws Exception {
	//
	// Use WMI to retrieve whatever information we can about services
	//
	String wql = "select * from Win32_Service";
	for (ISWbemObject obj : session.getWmiProvider().execQuery(IWmiProvider.CIMv2, wql)) {
	    ServiceItem item = Factories.sc.windows.createServiceItem();
	    ISWbemPropertySet props = obj.getProperties();

	    EntityItemStringType displayName =  Factories.sc.core.createEntityItemStringType();
	    displayName.setValue(props.getItem("DisplayName").getValueAsString());
	    item.setDisplayName(displayName);

	    EntityItemStringType description =  Factories.sc.core.createEntityItemStringType();
	    description.setValue(props.getItem("Description").getValueAsString());
	    item.setDescription(description);

	    EntityItemStringType startName =  Factories.sc.core.createEntityItemStringType();
	    startName.setValue(props.getItem("StartName").getValueAsString());
	    item.setStartName(startName);

	    EntityItemStringType path =  Factories.sc.core.createEntityItemStringType();
	    path.setValue(props.getItem("PathName").getValueAsString());
	    item.setPath(path);

	    EntityItemIntType pid =  Factories.sc.core.createEntityItemIntType();
	    pid.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    pid.setValue(props.getItem("ProcessId").getValueAsInteger().toString());
	    item.setPid(pid);

	    EntityItemBoolType flag = Factories.sc.core.createEntityItemBoolType();
	    flag.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    flag.setValue("0");
	    item.setServiceFlag(flag);

	    EntityItemServiceStartTypeType startType = Factories.sc.windows.createEntityItemServiceStartTypeType();
	    String startMode = props.getItem("StartMode").getValueAsString();
	    if ("Manual".equals(startMode)) {
		startType.setValue("SERVICE_DEMAND_START");
	    } else if ("Auto".equals(startMode)) {
		startType.setValue("SERVICE_AUTO_START");
	    } else if ("Disabled".equals(startMode)) {
		startType.setValue("SERVICE_DISABLED");
	    }

	    EntityItemServiceCurrentStateType state = Factories.sc.windows.createEntityItemServiceCurrentStateType();
	    state.setStatus(StatusEnumeration.NOT_COLLECTED);
	    item.setCurrentState(state);

	    services.put(props.getItem("Name").getValueAsString().toLowerCase(), item);
	}

	//
	// Use Powershell to get additional information
	//
	byte[] data = Base64.decode(getRunspace().invoke("List-ServiceInfo | Transfer-Encode"));
	ServiceItem currentItem = null;
	for (String line : new String(data, StringTools.UTF8).split("\r\n")) {
	    if (line.startsWith("Name: ")) {
		String name = line.substring(6);
		currentItem = services.get(name.toLowerCase());
		if (currentItem == null) {
		    session.getLogger().warn(JOVALMsg.WARNING_SERVICE, name);
		}
	    } else if (currentItem != null) {
		if (line.startsWith("State: ")) {
		    EntityItemServiceCurrentStateType stateType = currentItem.getCurrentState();
		    stateType.setStatus(StatusEnumeration.EXISTS);
		    int state = Integer.parseInt(line.substring(7), 16);
		    switch(state) {
		      case 1:
			stateType.setValue("SERVICE_STOPPED");
			break;
		      case 2:
			stateType.setValue("SERVICE_START_PENDING");
			break;
		      case 3:
			stateType.setValue("SERVICE_STOP_PENDING");
			break;
		      case 4:
			stateType.setValue("SERVICE_RUNNING");
			break;
		      case 5:
			stateType.setValue("SERVICE_CONTINUE_PENDING");
			break;
		      case 6:
			stateType.setValue("SERVICE_PAUSE_PENDING");
			break;
		      case 7:
			stateType.setValue("SERVICE_PAUSED");
			break;
		    }
		} else if (line.startsWith("Type Flags: ")) {
		    int flags = Integer.parseInt(line.substring(12), 16);
		    if (0x1 == (flags & 0x1)) {
			EntityItemServiceTypeType type = Factories.sc.windows.createEntityItemServiceTypeType();
			type.setValue("SERVICE_KERNEL_DRIVER");
			currentItem.getServiceType().add(type);
		    }
		    if (0x2 == (flags & 0x2)) {
			EntityItemServiceTypeType type = Factories.sc.windows.createEntityItemServiceTypeType();
			type.setValue("SERVICE_FILE_SYSTEM_DRIVER");
			currentItem.getServiceType().add(type);
		    }
		    if (0x10 == (flags & 0x10)) {
			EntityItemServiceTypeType type = Factories.sc.windows.createEntityItemServiceTypeType();
			type.setValue("SERVICE_WIN32_OWN_PROCESS");
			currentItem.getServiceType().add(type);
		    }
		    if (0x20 == (flags & 0x20)) {
			EntityItemServiceTypeType type = Factories.sc.windows.createEntityItemServiceTypeType();
			type.setValue("SERVICE_WIN32_SHARE_PROCESS");
			currentItem.getServiceType().add(type);
		    }
		    if (0x100 == (flags & 0x100)) {
			EntityItemServiceTypeType type = Factories.sc.windows.createEntityItemServiceTypeType();
			type.setValue("SERVICE_INTERACTIVE_PROCESS");
			currentItem.getServiceType().add(type);
		    }
		} else if (line.startsWith("Accept Flags: ")) {
		    int flags = Integer.parseInt(line.substring(14), 16);
		    if (0x1 == (flags & 0x1)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_STOP");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x2 == (flags & 0x2)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_PAUSE_CONTINUE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x4 == (flags & 0x4)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_SHUTDOWN");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x8 == (flags & 0x8)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_PARAMCHANGE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x10 == (flags & 0x10)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_NETBINDCHANGE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x20 == (flags & 0x20)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_HARDWAREPROFILECHANGE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x40 == (flags & 0x40)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_POWEREVENT");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x80 == (flags & 0x80)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_SESSIONCHANGE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x100 == (flags & 0x100)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_PRESHUTDOWN");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x200 == (flags & 0x200)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_TIMECHANGE");
			currentItem.getControlsAccepted().add(type);
		    }
		    if (0x400 == (flags & 0x400)) {
			EntityItemServiceControlsAcceptedType type =
				Factories.sc.windows.createEntityItemServiceControlsAcceptedType();
			type.setValue("SERVICE_ACCEPT_TRIGGEREVENT");
			currentItem.getControlsAccepted().add(type);
		    }
		} else if (line.startsWith("Requres: ")) {
		    EntityItemStringType dependency = Factories.sc.core.createEntityItemStringType();
		    dependency.setValue(line.substring(9));
		    currentItem.getDependencies().add(dependency);
		}
	    }
	}
    }
}
