// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

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

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.EntityObjectIPAddressStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.macos.InetlisteningserversObject;
import scap.oval.definitions.macos.Inetlisteningserver510Object;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.macos.InetlisteningserverItem;
import scap.oval.systemcharacteristics.macos.Inetlisteningserver510Item;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves macos:inetlisteningservers_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InetlisteningserversAdapter implements IAdapter {
    private IUnixSession session;
    private Collection<InetlisteningserverItem> portItems;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    IUnixSession us = (IUnixSession)session;
	    if (us.getFlavor() == IUnixSession.Flavor.MACOSX) {
		this.session = us;
		classes.add(InetlisteningserversObject.class);
		classes.add(Inetlisteningserver510Object.class);
	    } else {
		notapplicable.add(InetlisteningserversObject.class);
		notapplicable.add(Inetlisteningserver510Object.class);
	    }
	} else {
	    notapplicable.add(InetlisteningserversObject.class);
	    notapplicable.add(Inetlisteningserver510Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	try {
	    if (obj instanceof InetlisteningserversObject) {
		return getItems((InetlisteningserversObject)obj, rc);
	    } else if (obj instanceof Inetlisteningserver510Object) {
		return getItems((Inetlisteningserver510Object)obj, rc);
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Private

    private Collection<InetlisteningserverItem> getItems(InetlisteningserversObject iObj, IRequestContext rc)
		throws Exception {
	Collection<InetlisteningserverItem> items = new ArrayList<InetlisteningserverItem>();
	EntityObjectStringType programName = iObj.getProgramName();
	OperationEnumeration op = programName.getOperation();
	switch(op) {
	  case EQUALS:
	    for (InetlisteningserverItem item : portItems) {
		if (((String)programName.getValue()).equals((String)item.getProgramName().getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (InetlisteningserverItem item : portItems) {
		if (!((String)programName.getValue()).equals((String)item.getProgramName().getValue())) {
		    items.add(item);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile((String)programName.getValue());
	        for (InetlisteningserverItem item : portItems) {
		    if (p.matcher((String)item.getProgramName().getValue()).find()) {
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
	return items;
    }

    private Collection<Inetlisteningserver510Item> getItems(Inetlisteningserver510Object iObj, IRequestContext rc)
		throws Exception {

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
		Pattern p = Pattern.compile((String)localAddress.getValue());
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

	Collection<Inetlisteningserver510Item> result = new ArrayList<Inetlisteningserver510Item>();
	for (InetlisteningserverItem item : items) {
	    Inetlisteningserver510Item newItem = Factories.sc.macos.createInetlisteningserver510Item();
	    newItem.setProgramName(item.getProgramName());
	    newItem.setPid(item.getPid());

	    EntityItemIntType userId = Factories.sc.core.createEntityItemIntType();
	    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    userId.setValue((String)item.getUserId().getValue());
	    newItem.setUserId(userId);

	    newItem.setProtocol(item.getProtocol());
	    newItem.setLocalFullAddress(item.getLocalFullAddress());
	    newItem.setLocalAddress(item.getLocalAddress());
	    newItem.setLocalPort(item.getLocalPort());
	    newItem.setForeignFullAddress(item.getForeignFullAddress());
	    newItem.setForeignAddress(item.getForeignAddress());

	    EntityItemIntType foreignPort = Factories.sc.core.createEntityItemIntType();
	    foreignPort.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    foreignPort.setStatus(item.getForeignPort().getStatus());
	    foreignPort.setValue((String)item.getForeignPort().getValue());
	    newItem.setForeignPort(foreignPort);

	    result.add(newItem);
	}
	return result;
    }

    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (portItems != null) {
	    return;
	}
	try {
	    //
	    // Find process owners and names
	    //
	    Map<Integer, String> processNames = new HashMap<Integer, String>();
	    Iterator<String> lines = SafeCLI.multiLine("ps -wwAo pid,args", session, IUnixSession.Timeout.S).iterator();
	    boolean first = true;
	    while(lines.hasNext()) {
		String line = lines.next();
		if (first) {
		    first = false;
		    continue;
		}
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 2) {
		    Integer pid = new Integer(tok.nextToken());
		    processNames.put(pid, tok.nextToken("\n").trim());
		}
	    }

	    //
	    // List TCP connections
	    //
	    Collection<String> data = new ArrayList<String>();
	    long timeout = session.getTimeout(ISession.Timeout.S);
	    SafeCLI.ExecData ed = SafeCLI.execData("lsof -i TCP -n -l -P", null, session, timeout);
	    first = true;
	    switch(ed.getExitCode()) {
	      case 0:
		for (String line : ed.getLines()) {
		    if (first) {
			first = false;
		    } else if (line.endsWith("(ESTABLISHED)") || line.endsWith("(LISTEN)")) {
			data.add(line);
		    }
	    	}
		break;
	      default:
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_LSOF, Integer.toString(ed.getExitCode())));
	    }
	    ed = SafeCLI.execData("lsof -i UDP -n -l -P", null, session, timeout);
	    first = true;
	    switch(ed.getExitCode()) {
	      case 0:
		for (String line : ed.getLines()) {
		    if (first) {
			first = false;
		    } else {
			data.add(line);
		    }
	    	}
		break;
	      default:
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_LSOF, Integer.toString(ed.getExitCode())));
	    }

	    portItems = new ArrayList<InetlisteningserverItem>();
	    lines = data.iterator();
	    while(lines.hasNext()) {
		String line = lines.next().trim();
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 9) {
		    String s = tok.nextToken();
		    InetlisteningserverItem item = Factories.sc.macos.createInetlisteningserverItem();

		    Integer pid = new Integer(tok.nextToken());
		    EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
		    pidType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    pidType.setValue(pid.toString());
		    item.setPid(pidType);

		    EntityItemStringType programName = Factories.sc.core.createEntityItemStringType();
		    programName.setValue(processNames.get(pid));
		    item.setProgramName(programName);

		    EntityItemStringType userId = Factories.sc.core.createEntityItemStringType();
		    userId.setValue(tok.nextToken());
		    item.setUserId(userId);

		    String fd = tok.nextToken();
		    boolean ip6 = "IPv6".equals(tok.nextToken()); // else IPv4
		    String device = tok.nextToken();
		    String szOff = tok.nextToken();

		    String protocol = tok.nextToken().toLowerCase();
		    EntityItemStringType protocolType = Factories.sc.core.createEntityItemStringType();
		    protocolType.setValue(protocol);
		    item.setProtocol(protocolType);

		    String local=null, foreign=null;
		    if ("udp".equals(protocol)) {
			// There is no such thing as a foreign UDP port
			local = tok.nextToken();
		    } else {
			// TCP
			String addresses = tok.nextToken();
			String tcp_state = tok.nextToken();

			if ("(ESTABLISHED)".equals(tcp_state)) {
			    int ptr = addresses.indexOf("->");
			    local = addresses.substring(0,ptr);
			    foreign = addresses.substring(0,ptr+2);
			} else {
			    local = addresses;
			    foreign = ip6 ? "[::]:0" : "0.0.0.0:0"; // listening is defined as having these foreign addrs
			}
		    }

		    EntityItemStringType localFullAddress = Factories.sc.core.createEntityItemStringType();
		    localFullAddress.setValue(local);
		    item.setLocalFullAddress(localFullAddress);

		    int ptr = local.lastIndexOf(":");
		    EntityItemIPAddressStringType localAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    if (ip6) {
			localAddress.setDatatype(SimpleDatatypeEnumeration.IPV_6_ADDRESS.value());
			if ("*".equals(local.substring(0,ptr))) {
			    localAddress.setValue("::");
			} else {
			    localAddress.setValue(local.substring(1,ptr-1)); // trim [ and ]
			}
		    } else {
			localAddress.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
			String addr = local.substring(0,ptr);
			if ("*".equals(addr)) {
			    localAddress.setValue("0.0.0.0");
			} else {
			    localAddress.setValue(addr);
			}
		    }
		    item.setLocalAddress(localAddress);

		    EntityItemIntType localPort = Factories.sc.core.createEntityItemIntType();
		    localPort.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    String portNum = local.substring(ptr+1);
		    if ("*".equals(portNum)) {
			portNum = "0";
		    }
		    localPort.setValue(portNum);
		    item.setLocalPort(localPort);

		    EntityItemStringType foreignFullAddress = Factories.sc.core.createEntityItemStringType();
		    item.setForeignFullAddress(foreignFullAddress);
		    EntityItemIPAddressStringType foreignAddress = Factories.sc.core.createEntityItemIPAddressStringType();
		    foreignAddress.setDatatype(ip6 ? SimpleDatatypeEnumeration.IPV_6_ADDRESS.value() :
						     SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
		    item.setForeignAddress(foreignAddress);
		    EntityItemStringType foreignPort = Factories.sc.core.createEntityItemStringType();
		    item.setForeignPort(foreignPort);
		    if (foreign == null) {
			foreignFullAddress.setStatus(StatusEnumeration.DOES_NOT_EXIST);
			foreignAddress.setStatus(StatusEnumeration.DOES_NOT_EXIST);
			foreignPort.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		    } else {
			foreignFullAddress.setValue(foreign);
			ptr = foreign.lastIndexOf(":");
			if (ip6) {
			    foreignAddress.setValue(foreign.substring(1,ptr-1)); // trim [ and ]
			} else {
			    foreignAddress.setValue(foreign.substring(0,ptr));
			}
			foreignPort.setValue(foreign.substring(ptr+1));
		    }

		    portItems.add(item);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
    }
}
