// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectIPAddressStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.PortObject;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.PortItem;
import scap.oval.systemcharacteristics.windows.EntityItemProtocolType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:port_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PortAdapter implements IAdapter {
    protected IWindowsSession session;
    private Collection<PortItem> portItems = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(PortObject.class);
	} else {
	    notapplicable.add(PortObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	PortObject pObj = (PortObject)obj;

	//
	// Create a list of items with matching addresses
	//
	Collection<PortItem> items = new ArrayList<PortItem>();
	EntityObjectIPAddressStringType localAddress = pObj.getLocalAddress();
	OperationEnumeration op = localAddress.getOperation();
	switch(op) {
	  case EQUALS:
	    for (PortItem item : portItems) {
		if (item.getLocalAddress().getValue().equals(localAddress.getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (PortItem item : portItems) {
		if (!item.getLocalAddress().getValue().equals(localAddress.getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile((String)localAddress.getValue());
		for (PortItem item : portItems) {
		    if (p.matcher((String)item.getLocalAddress().getValue()).find()) {
			items.add(item);
		    }
		}
	    } catch (PatternSyntaxException e) {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	//
	// Remove items not matching the protocol
	//
	Iterator<PortItem> iter = items.iterator();
	String protocol = (String)pObj.getProtocol().getValue();
	while(iter.hasNext()) {
	    PortItem item = iter.next();
	    String itemProtocol = (String)item.getProtocol().getValue();
	    op = pObj.getProtocol().getOperation();
	    switch(op) {
	      case EQUALS:
		if (!protocol.equals(itemProtocol)) {
		    iter.remove();
		}
		break;

	      case NOT_EQUAL:
		if (protocol.equals(itemProtocol)) {
		    iter.remove();
		}
		break;

	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(protocol);
		    if (!p.matcher(itemProtocol).find()) {
			iter.remove();
		    }
		} catch (PatternSyntaxException e) {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		    throw new CollectException(msg, FlagEnumeration.ERROR);
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	//
	// Remove items not matching the port
	//
	iter = items.iterator();
	int port = Integer.parseInt((String)pObj.getLocalPort().getValue());
	while(iter.hasNext()) {
	    PortItem item = iter.next();
	    int itemPort = Integer.parseInt((String)item.getLocalPort().getValue());
	    op = pObj.getLocalPort().getOperation();
	    switch(op) {
	      case EQUALS:
		if (port != itemPort) {
		    iter.remove();
		}
		break;

	      case NOT_EQUAL:
		if (port == itemPort) {
		    iter.remove();
		}
		break;

	      case GREATER_THAN:
		if (port >= itemPort) {
		    iter.remove();
		}
		break;

	      case GREATER_THAN_OR_EQUAL:
		if (port > itemPort) {
		    iter.remove();
		}
		break;

	      case LESS_THAN:
		if (port <= itemPort) {
		    iter.remove();
		}
		break;

	      case LESS_THAN_OR_EQUAL:
		if (port < itemPort) {
		    iter.remove();
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	return items;
    }

    // Private

    private void init() throws CollectException {
	if (portItems != null) {
	    return;
	}
	try {
	    Iterator<String> lines = SafeCLI.multiLine("netstat /ano", session, IWindowsSession.Timeout.S).iterator();
	    portItems = new ArrayList<PortItem>();
	    while(lines.hasNext()) {
		StringTokenizer tok = new StringTokenizer(lines.next());
		if (tok.countTokens() >= 4) {
		    PortItem item = Factories.sc.windows.createPortItem();

		    String protocol = tok.nextToken();
		    if (!"TCP".equals(protocol) && !"UDP".equals(protocol)) {
			continue; // probably the header row
		    }

		    EntityItemProtocolType protocolType = Factories.sc.windows.createEntityItemProtocolType();
		    protocolType.setValue(protocol);
		    item.setProtocol(protocolType);

		    String local = tok.nextToken();
		    int ptr = local.lastIndexOf(":");
		    EntityItemIPAddressStringType localAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    if (local.startsWith("[") && local.charAt(ptr-1) == ']') {
			int ptr2 = local.indexOf("%");
			localAddress.setValue(local.substring(1,ptr2 == -1 ? ptr-1 : ptr2));
			localAddress.setDatatype(SimpleDatatypeEnumeration.IPV_6_ADDRESS.value());
		    } else {
			localAddress.setValue(local.substring(0,ptr));
			localAddress.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
		    }
		    item.setLocalAddress(localAddress);

		    EntityItemIntType localPort = Factories.sc.core.createEntityItemIntType();
		    localPort.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    localPort.setValue(local.substring(ptr+1));
		    item.setLocalPort(localPort);

		    String foreign = tok.nextToken();
		    ptr = foreign.lastIndexOf(":");
		    EntityItemIPAddressStringType foreignAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    if (foreign.startsWith("[") && foreign.charAt(ptr-1) == ']') {
			int ptr2 = foreign.indexOf("%");
			foreignAddress.setValue(foreign.substring(1,ptr2 == -1 ? ptr-1 : ptr2));
			foreignAddress.setDatatype(SimpleDatatypeEnumeration.IPV_6_ADDRESS.value());
		    } else if (foreign.startsWith("*")) {
			foreignAddress.setValue(foreign.substring(0,ptr));
			foreignAddress.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		    } else {
			foreignAddress.setValue(foreign.substring(0,ptr));
			foreignAddress.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
		    }
		    item.setForeignAddress(foreignAddress);

		    if ("TCP".equals(protocol)) {
			String tcp_state = tok.nextToken();
		    }

		    EntityItemStringType foreignPort = Factories.sc.core.createEntityItemStringType();
		    foreignPort.setValue(foreign.substring(ptr+1));
		    item.setForeignPort(foreignPort);

		    EntityItemIntType pid = Factories.sc.core.createEntityItemIntType();
		    pid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    pid.setValue(tok.nextToken());
		    item.setPid(pid);

		    portItems.add(item);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
    }
}
