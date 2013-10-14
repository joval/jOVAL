// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectIPAddressStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.InetlisteningserversObject;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.linux.InetlisteningserverItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves linux:inetlisteningservers_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InetlisteningserversAdapter implements IAdapter {
    protected IUnixSession session;
    private Collection<InetlisteningserverItem> portItems = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    IUnixSession us = (IUnixSession)session;
	    if (us.getFlavor() == IUnixSession.Flavor.LINUX) {
		this.session = us;
		classes.add(InetlisteningserversObject.class);
	    } else {
		notapplicable.add(InetlisteningserversObject.class);
	    }
	} else {
	    notapplicable.add(InetlisteningserversObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	InetlisteningserversObject iObj = (InetlisteningserversObject)obj;

	//
	// Create a list of items with matching addresses
	//
	Collection<InetlisteningserverItem> items = new ArrayList<InetlisteningserverItem>();
	EntityObjectIPAddressStringType localAddress = iObj.getLocalAddress();
	OperationEnumeration op = localAddress.getOperation();
	switch(op) {
	  case EQUALS:
	    for (InetlisteningserverItem item : portItems) {
		if (item.getLocalAddress().getValue().equals(localAddress.getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (InetlisteningserverItem item : portItems) {
		if (!item.getLocalAddress().getValue().equals(localAddress.getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = StringTools.pattern((String)localAddress.getValue());
		for (InetlisteningserverItem item : portItems) {
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
	Iterator<InetlisteningserverItem> iter = items.iterator();
	String protocol = (String)iObj.getProtocol().getValue();
	while(iter.hasNext()) {
	    InetlisteningserverItem item = iter.next();
	    String itemProtocol = (String)item.getProtocol().getValue();
	    op = iObj.getProtocol().getOperation();
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
		    Pattern p = StringTools.pattern(protocol);
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
	int port = Integer.parseInt((String)iObj.getLocalPort().getValue());
	while(iter.hasNext()) {
	    InetlisteningserverItem item = iter.next();
	    int itemPort = Integer.parseInt((String)item.getLocalPort().getValue());
	    op = iObj.getLocalPort().getOperation();
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
	    //
	    // Find process owners
	    //
	    Map<Integer, Integer> processOwners = new HashMap<Integer, Integer>();
	    Iterator<String> lines = SafeCLI.multiLine("ps -eo pid,uid", session, IUnixSession.Timeout.S).iterator();
	    while(lines.hasNext()) {
		StringTokenizer tok = new StringTokenizer(lines.next());
		if (tok.countTokens() == 2) {
		    String pid = tok.nextToken();
		    String uid = tok.nextToken();
		    try {
			processOwners.put(new Integer(pid), new Integer(uid));
		    } catch (NumberFormatException e) {
			// ignore the header
		    }
		}
	    }

	    //
	    // List the listeners and established TCP connections
	    //
	    portItems = new ArrayList<InetlisteningserverItem>();
	    Collection<String> data = new ArrayList<String>();
	    data.addAll(SafeCLI.multiLine("netstat -lnptu", session, IUnixSession.Timeout.S));
	    data.addAll(SafeCLI.multiLine("netstat -npt | grep ESTABLISHED", session, IUnixSession.Timeout.S));
	    lines = data.iterator();
	    while(lines.hasNext()) {
		String line = lines.next().trim();
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 6) {
		    InetlisteningserverItem item = Factories.sc.linux.createInetlisteningserverItem();

		    String protocol = tok.nextToken();
		    if (!"tcp".equals(protocol) && !"udp".equals(protocol)) {
			continue; // a header or message row
		    }

		    EntityItemStringType protocolType = Factories.sc.core.createEntityItemStringType();
		    protocolType.setValue(protocol);
		    item.setProtocol(protocolType);

		    String recvq = tok.nextToken();
		    String sendq = tok.nextToken();

		    String local = tok.nextToken();
		    EntityItemStringType localFullAddress = Factories.sc.core.createEntityItemStringType();
		    localFullAddress.setValue(local);
		    item.setLocalFullAddress(localFullAddress);

		    int ptr = local.lastIndexOf(":");
		    boolean ip6 = local.indexOf(":") < ptr;

		    EntityItemIPAddressStringType localAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    localAddress.setDatatype(ip6 ? SimpleDatatypeEnumeration.IPV_6_ADDRESS.value() :
						   SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
		    localAddress.setValue(local.substring(0,ptr));
		    item.setLocalAddress(localAddress);

		    EntityItemIntType localPort = Factories.sc.core.createEntityItemIntType();
		    localPort.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    localPort.setValue(local.substring(ptr+1));
		    item.setLocalPort(localPort);

		    String foreign = tok.nextToken();
		    EntityItemStringType foreignFullAddress = Factories.sc.core.createEntityItemStringType();
		    foreignFullAddress.setValue(foreign);
		    item.setForeignFullAddress(foreignFullAddress);

		    ptr = foreign.lastIndexOf(":");
		    EntityItemIPAddressStringType foreignAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    foreignAddress.setDatatype(ip6 ? SimpleDatatypeEnumeration.IPV_6_ADDRESS.value() :
						     SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
		    foreignAddress.setValue(foreign.substring(0,ptr));
		    item.setForeignAddress(foreignAddress);

		    if ("tcp".equals(protocol)) {
			String tcp_state = tok.nextToken();
		    }

		    EntityItemIntType foreignPort = Factories.sc.core.createEntityItemIntType();
		    foreignPort.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    String foreignPortNumber = foreign.substring(ptr+1);
		    if ("*".equals(foreignPortNumber)) {
			foreignPortNumber = "0";
		    }
		    foreignPort.setValue(foreignPortNumber);
		    item.setForeignPort(foreignPort);

		    String processInfo = tok.nextToken();
		    ptr = processInfo.indexOf("/");
		    Integer pid = new Integer(processInfo.substring(0,ptr));
		    EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
		    pidType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    pidType.setValue(pid.toString());
		    item.setPid(pidType);

		    EntityItemIntType userId = Factories.sc.core.createEntityItemIntType();
		    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    userId.setValue(processOwners.get(pid).toString());
		    item.setUserId(userId);
		    portItems.add(item);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
    }
}
