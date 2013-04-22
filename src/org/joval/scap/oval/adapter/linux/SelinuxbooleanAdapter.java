// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.SelinuxbooleanObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.linux.SelinuxbooleanItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects Selinuxboolean OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SelinuxbooleanAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, SelinuxbooleanItem> booleanMap;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.LINUX) {
	    this.session = (IUnixSession)session;
	    booleanMap = new Hashtable<String, SelinuxbooleanItem>();
	    classes.add(SelinuxbooleanObject.class);
	} else {
	    notapplicable.add(SelinuxbooleanObject.class);
	}
	return classes;
    }

    public Collection<SelinuxbooleanItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	SelinuxbooleanObject bObj = (SelinuxbooleanObject)obj;
	Collection<SelinuxbooleanItem> items = new ArrayList<SelinuxbooleanItem>();
	switch(bObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem(SafeCLI.checkArgument((String)bObj.getName().getValue(), session)));
	    } catch (NoSuchElementException e) {
		// there is no such boolean; don't add to the item list
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SELINUX_BOOL, (String)bObj.getName().getValue(), e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
		session.getLogger().warn(s, e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadBooleans();
	    try {
		Pattern p = StringTools.pattern((String)bObj.getName().getValue());
		for (String booleanName : booleanMap.keySet()) {
		    if (p.matcher(booleanName).find()) {
			items.add(booleanMap.get(booleanName));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadBooleans();
	    String name = (String)bObj.getName().getValue();
	    for (String booleanName : booleanMap.keySet()) {
		if (!booleanName.equals(name)) {
		    items.add(booleanMap.get(booleanName));
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, bObj.getName().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private SelinuxbooleanItem getItem(String name) throws Exception {
	SelinuxbooleanItem item = booleanMap.get(name);
	if (item == null) {
	    item = parseBoolean(SafeCLI.exec("/usr/sbin/getsebool '" + name + "'", session, IUnixSession.Timeout.S));
	    if (item != null) {
		booleanMap.put(name, item);
	    }
	}
	if (item == null) {
	    throw new NoSuchElementException(name);
	} else {
	    return item;
	}
    }

    private boolean loaded = false;
    private void loadBooleans() {
	if (!loaded) {
	    try {
		for (String line : SafeCLI.multiLine("/usr/sbin/getsebool -a", session, IUnixSession.Timeout.S)) {
		    SelinuxbooleanItem item = parseBoolean(line);
		    if (item != null) {
			booleanMap.put((String)item.getName().getValue(), item);
		    }
		}
		loaded = true;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    private static final String DELIM1	= ">";		// early version of the delimiter
    private static final String DELIM2	= "-->";	// delimiter since FC5 release
    private static final String PENDING	= "pending:";	// delimiter for pending state

    private SelinuxbooleanItem parseBoolean(String line) {
	String name = null, value = null;
	int ptr = line.indexOf(DELIM2);
	if (ptr > 0) {
	    name = line.substring(0,ptr).trim();
	    value = line.substring(ptr+4).trim();
	} else {
	    ptr = line.indexOf(DELIM1);
	    if (ptr > 0) {
		name = line.substring(0,ptr).trim();
		value = line.substring(ptr+1).trim();
	    }
	}
	if (name == null) {
	    return null;
	} else {
	    SelinuxbooleanItem item = Factories.sc.linux.createSelinuxbooleanItem();

	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    nameType.setValue(name);
	    item.setName(nameType);

	    EntityItemBoolType pendingStatus = Factories.sc.core.createEntityItemBoolType();
	    pendingStatus.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    ptr = value.indexOf(PENDING);
	    if (ptr > 0) {
		String pending = value.substring(0,ptr).trim();
		value = value.substring(ptr+8).trim();

		if ("on".equals(value) || "active".equals(value)) {
		    pendingStatus.setValue("1");
		} else {
		    pendingStatus.setValue("0");
		}
	    }
	    item.setPendingStatus(pendingStatus);

	    EntityItemBoolType currentStatus = Factories.sc.core.createEntityItemBoolType();
	    currentStatus.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    if ("on".equals(value) || "active".equals(value)) {
		currentStatus.setValue("1");
	    } else {
		currentStatus.setValue("0");
	    }
	    item.setCurrentStatus(currentStatus);

	    if (!pendingStatus.isSetValue()) {
		pendingStatus.setValue(currentStatus.getValue());
	    }

	    return item;
	}
    }
}
