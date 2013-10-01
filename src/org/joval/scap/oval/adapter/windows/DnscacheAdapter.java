// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.DnscacheObject;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressStringType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.DnscacheItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:dnscache_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DnscacheAdapter implements IAdapter {
    protected IWindowsSession session;
    private Map<String, DnscacheItem> dnsCache = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(DnscacheObject.class);
	} else {
	    notapplicable.add(DnscacheObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	Collection<DnscacheItem> items = new ArrayList<DnscacheItem>();
	DnscacheObject dObj = (DnscacheObject)obj;
	String domainName = (String)dObj.getDomainName().getValue();
	OperationEnumeration op = dObj.getDomainName().getOperation();
	switch(op) {
	  case EQUALS:
	    if (dnsCache.containsKey(domainName)) {
		items.add(dnsCache.get(domainName));
	    }
	    break;

	  case NOT_EQUAL:
	    for (Map.Entry<String, DnscacheItem> entry : dnsCache.entrySet()) {
		if (!domainName.equals(entry.getKey())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(domainName);
		for (Map.Entry<String, DnscacheItem> entry : dnsCache.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue());
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

    // Private

    private void init() throws CollectException {
	if (dnsCache != null) {
	    return;
	}
	try {
	    Iterator<String> lines = SafeCLI.multiLine("ipconfig /displaydns", session, IWindowsSession.Timeout.S).iterator();
	    DnscacheItem currentItem = null;
	    SimpleDatatypeEnumeration datatype = null;
	    dnsCache = new HashMap<String, DnscacheItem>();
	    while(lines.hasNext()) {
		String line = lines.next().trim();
		if (line.length() == 0) {
		    if (currentItem != null) {
			if (currentItem.isSetIpAddress()) {
			    dnsCache.put((String)currentItem.getDomainName().getValue(), currentItem);
			}
			currentItem = null;
			datatype = null;
		    }
		} else if (line.startsWith("Record Name")) {
		    int ptr = line.indexOf(":");
		    String recordName = line.substring(ptr+1).trim();
		    if (dnsCache.containsKey(recordName)) {
			currentItem = dnsCache.get(recordName);
		    } else {
			currentItem = Factories.sc.windows.createDnscacheItem();
			EntityItemStringType domainName = Factories.sc.core.createEntityItemStringType();
			domainName.setValue(recordName);
			currentItem.setDomainName(domainName);
		    }
		} else if (currentItem != null) {
		    if (line.startsWith("Record Type")) {
			int ptr = line.indexOf(":");
			switch(Integer.parseInt(line.substring(ptr+1).trim())) {
			  case 1:
			    datatype = SimpleDatatypeEnumeration.IPV_4_ADDRESS;
			    break;
			  case 28:
			    datatype = SimpleDatatypeEnumeration.IPV_6_ADDRESS;
			    break;
			  default:
			    datatype = null;
			    break;
			}
		    } else if (line.startsWith("A") && line.indexOf("Record") != -1 && datatype != null) {
			EntityItemIPAddressStringType ipAddress = Factories.sc.core.createEntityItemIPAddressStringType();
			int ptr = line.indexOf(":");
			switch(datatype) {
			  case IPV_4_ADDRESS:
			    ipAddress.setDatatype(datatype.value());
			    ipAddress.setValue(line.substring(ptr+1).trim());
			    currentItem.getIpAddress().add(ipAddress);
			    break;

			  case IPV_6_ADDRESS:
			    ipAddress.setDatatype(datatype.value());
			    String addr = line.substring(ptr+1).trim();
			    if (addr.startsWith("[") && addr.endsWith("]")) {
				addr = addr.substring(1, addr.length() - 1);
			    }
			    ptr = addr.indexOf("%");
			    if (ptr != -1) {
				addr = addr.substring(0,ptr);
			    }
			    ipAddress.setValue(addr);
			    currentItem.getIpAddress().add(ipAddress);
			    break;
			}
			datatype = null;
		    } else if (line.startsWith("Time To Live")) {
			int ptr = line.indexOf(":");
			EntityItemIntType ttl = Factories.sc.core.createEntityItemIntType();
			ttl.setDatatype(SimpleDatatypeEnumeration.INT.value());
			ttl.setValue(line.substring(ptr+1).trim());
			currentItem.setTtl(ttl);
		    }
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
    }
}
