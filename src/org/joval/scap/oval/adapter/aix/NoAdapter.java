// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.aix.NoObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.aix.NoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves items for AIX fileset_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NoAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, String> tunables;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.AIX) {
	    this.session = (IUnixSession)session;
	    classes.add(NoObject.class);
	} else {
	    notapplicable.add(NoObject.class);
	}
	return classes;
    }

    public Collection<NoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	NoObject nObj = (NoObject)obj;
	try {
	    Collection<NoItem> items = new ArrayList<NoItem>();
	    switch(nObj.getTunable().getOperation()) {
	      case EQUALS:
		try {
		    items.add(getItem((String)nObj.getTunable().getValue()));
		} catch (NoSuchElementException e) {
		    // we'll return an empty list
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)nObj.getTunable().getValue());
		for (String tunable : getTunables().keySet()) {
		    if (p.matcher(tunable).find()) {
			try {
			    items.add(getItem(tunable));
			} catch (NoSuchElementException e) {
			}
		    }
		}
		break;

	      case NOT_EQUAL:
		for (String tunable : getTunables().keySet()) {
		    if (!tunable.equals((String)nObj.getTunable().getValue())) {
			try {
			    items.add(getItem(tunable));
			} catch (NoSuchElementException e) {
			}
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, nObj.getTunable().getOperation());
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

    private Map<String, String> getTunables() throws Exception {
	if (tunables == null) {
	    try {
		Iterator<String> iter = SafeCLI.manyLines("/etc/no -a", null, session);
		tunables = new HashMap<String, String>();
		String line = null;
		while(iter.hasNext()) {
		    line = iter.next();
		    int ptr = line.indexOf(" = ");
		    String key = line.substring(0,ptr).trim();
		    String val = line.substring(ptr+3);
		    tunables.put(key, val);
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return tunables;
    }

    private NoItem getItem(String tunable) throws Exception {
	if (getTunables().containsKey(tunable)) {
	    NoItem item = Factories.sc.aix.createNoItem();

	    EntityItemStringType tunableType = Factories.sc.core.createEntityItemStringType();
	    tunableType.setValue(tunable);
	    item.setTunable(tunableType);

	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    valueType.setValue(getTunables().get(tunable));
	    item.setValue(valueType);

	    return item;
	} else {
	    throw new NoSuchElementException(tunable);
	}
    }
}
