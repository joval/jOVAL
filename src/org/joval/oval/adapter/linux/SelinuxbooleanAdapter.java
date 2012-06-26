// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.linux;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.linux.SelinuxbooleanObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.linux.SelinuxbooleanItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.Version;

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

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case LINUX:
		booleanMap = new Hashtable<String, SelinuxbooleanItem>();
		classes.add(SelinuxbooleanObject.class);
		break;
	    }
	}
	return classes;
    }

    public Collection<SelinuxbooleanItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	SelinuxbooleanObject bObj = (SelinuxbooleanObject)obj;
	Collection<SelinuxbooleanItem> items = new Vector<SelinuxbooleanItem>();
	switch(bObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem((String)bObj.getName().getValue()));
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
		Pattern p = Pattern.compile((String)bObj.getName().getValue());
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
	    item = parseBoolean(SafeCLI.exec("/usr/sbin/getsebool " + name, session, IUnixSession.Timeout.S));
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

    private static final String DELIM = "-->";

    private SelinuxbooleanItem parseBoolean(String line) {
	int ptr = line.indexOf(DELIM);
	if (ptr > 0) {
	    SelinuxbooleanItem item = Factories.sc.linux.createSelinuxbooleanItem();

	    String name = line.substring(0,ptr).trim();
	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    nameType.setValue(name);
	    item.setName(nameType);

	    String value = line.substring(ptr+4).trim();
	    EntityItemBoolType currentStatus = Factories.sc.core.createEntityItemBoolType();
	    currentStatus.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    if ("on".equals(value)) {
		currentStatus.setValue("1");
	    } else {
		currentStatus.setValue("0");
	    }
	    item.setCurrentStatus(currentStatus);

	    return item;
	} else {
	    return null;
	}
    }
}
