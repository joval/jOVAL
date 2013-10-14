// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.InterfaceObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemAddrTypeType;
import scap.oval.systemcharacteristics.windows.EntityItemInterfaceTypeType;
import scap.oval.systemcharacteristics.windows.InterfaceItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.sysinfo.NetworkInterface;
import org.joval.util.JOVALMsg;

/**
 * Resolves Interface OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InterfaceAdapter implements IAdapter {
    private IWindowsSession session;
    private Map<String, Collection<InterfaceItem>> interfaces;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(InterfaceObject.class);
	} else {
	    notapplicable.add(InterfaceObject.class);
	}
	return classes;
    }

    public Collection<InterfaceItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	try {
	    init();
	    InterfaceObject iObj = (InterfaceObject)obj;
	    String name = (String)iObj.getName().getValue();
	    OperationEnumeration op = iObj.getName().getOperation();
	    Collection<InterfaceItem> items = new ArrayList<InterfaceItem>();
	    switch(op) {
	      case EQUALS:
		if (interfaces.containsKey(name)) {
		    items.addAll(interfaces.get(name));
		}
		break;
	      case CASE_INSENSITIVE_EQUALS:
		for (Map.Entry<String, Collection<InterfaceItem>> entry : interfaces.entrySet()) {
		    if (entry.getKey().equalsIgnoreCase(name)) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      case NOT_EQUAL:
		for (Map.Entry<String, Collection<InterfaceItem>> entry : interfaces.entrySet()) {
		    if (!entry.getKey().equals(name)) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern(name);
		for (Map.Entry<String, Collection<InterfaceItem>> entry : interfaces.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    return items;
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Private

    /**
     * Idempotent.
     */
    private void init() throws Exception {
	if (interfaces != null) {
	    return;
	}
	interfaces = new HashMap<String, Collection<InterfaceItem>>();
	List<NetworkInterface> intfs = NetworkInterface.getInterfaces(session);
	for (int i=0; i < intfs.size(); i++) {
	    NetworkInterface intf = intfs.get(i);
	    if (intf == null) {
		continue;
	    }

	    EntityItemIntType indexType = Factories.sc.core.createEntityItemIntType();
	    indexType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    indexType.setValue(Integer.toString(i));

	    EntityItemInterfaceTypeType typeType = Factories.sc.windows.createEntityItemInterfaceTypeType();
	    if (intf.getType() == null) {
		typeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    } else {
		switch(intf.getType()) {
		  case ETHER:
		    typeType.setValue("MIB_IF_TYPE_ETHERNET");
		    break;
		  case LOOPBACK:
		    typeType.setValue("MIB_IF_TYPE_LOOPBACK");
		    break;
		  case FDDI:
		    typeType.setValue("MIB_IF_TYPE_FDDI");
		    break;
		  case TOKENRING:
		    typeType.setValue("MIB_IF_TYPE_TOKENRING");
		    break;
		  default:
		    typeType.setValue("MIB_IF_TYPE_OTHER");
		    break;
		}
	    }

	    String name = intf.getName();
	    if (!interfaces.containsKey(name)) {
		interfaces.put(name, new ArrayList<InterfaceItem>());
	    }

	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setValue(name);

	    EntityItemStringType hardwareAddr = Factories.sc.core.createEntityItemStringType();
	    if (intf.getHardwareAddress() == null) {
		hardwareAddr.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		hardwareAddr.setValue(intf.getHardwareAddress());
	    }

	    EntityItemAddrTypeType addrType = Factories.sc.windows.createEntityItemAddrTypeType();
	    addrType.setStatus(StatusEnumeration.NOT_COLLECTED);

	    if (intf.getIPAddresses() != null && intf.getIPAddresses().size() > 0) {
		for (NetworkInterface.IPAddress addr : intf.getIPAddresses()) {
		    InterfaceItem item = Factories.sc.windows.createInterfaceItem();
		    item.setIndex(indexType);
		    item.setType(typeType);
		    item.setName(nameType);
		    item.setHardwareAddr(hardwareAddr);
		    item.getAddrType().add(addrType);

		    EntityItemIPAddressStringType inetAddr = Factories.sc.core.createEntityItemIPAddressStringType();
		    switch(addr.getVersion()) {
		      case V4:
			inetAddr.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
			inetAddr.setValue(addr.getAddress());
			if (addr.getBroadcast() != null) {
			    EntityItemIPAddressStringType broadcast = Factories.sc.core.createEntityItemIPAddressStringType();
			    broadcast.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
			    broadcast.setValue(addr.getBroadcast());
			    item.setBroadcastAddr(broadcast);
			}
			if (addr.getMask() != null) {
			    EntityItemIPAddressStringType mask = Factories.sc.core.createEntityItemIPAddressStringType();
			    mask.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
			    mask.setValue(addr.getMask());
			    item.setNetmask(mask);
			}
			break;

		      case V6:
			inetAddr.setDatatype(SimpleDatatypeEnumeration.IPV_6_ADDRESS.value());
System.out.println("DAS IP6: " + addr.getAddress() + "/" + addr.getMask());
			if (addr.getMask() == null) {
			    inetAddr.setValue(addr.getAddress());
			} else {
			    inetAddr.setValue(addr.getAddress() + "/" + addr.getMask());
			}
			break;
		    }
		    item.setInetAddr(inetAddr);

		    interfaces.get(name).add(item);
		}
	    } else {
		InterfaceItem item = Factories.sc.windows.createInterfaceItem();
		item.setIndex(indexType);
		item.setType(typeType);
		item.setName(nameType);
		item.setHardwareAddr(hardwareAddr);
		item.getAddrType().add(addrType);
		EntityItemIPAddressStringType inetAddr = Factories.sc.core.createEntityItemIPAddressStringType();
		inetAddr.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.setInetAddr(inetAddr);
		interfaces.get(name).add(item);
	    }
	}
    }
}
