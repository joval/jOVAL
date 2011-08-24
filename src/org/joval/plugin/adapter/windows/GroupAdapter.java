// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.windows.GroupObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.GroupItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.identity.windows.LocalDirectory;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * Evaluates Group OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GroupAdapter implements IAdapter {
    private IWmiProvider wmi;
    private String hostname;
    private LocalDirectory local = null;

    public GroupAdapter(String hostname, IWmiProvider wmi) {
	this.wmi = wmi;
	this.hostname = hostname.toUpperCase();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return GroupObject.class;
    }

    public boolean connect() {
	if (wmi.connect()) {
	    local = new LocalDirectory(hostname, wmi);
	    return true;
	}
	return false;
    }

    public void disconnect() {
	wmi.disconnect();
	local = null;
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	GroupObject gObj = (GroupObject)rc.getObject();
	String group = (String)gObj.getGroup().getValue();

	try {
	    switch(gObj.getGroup().getOperation()) {
	      case EQUALS:
		items.add(JOVALSystem.factories.sc.windows.createGroupItem(local.queryGroup(group)));
		break;
    
	      case NOT_EQUAL:
		for (GroupItem item : local.queryAllGroups()) {
		    if (!group.equals((String)item.getGroup().getValue())) {
			items.add(JOVALSystem.factories.sc.windows.createGroupItem(item));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(group);
		    for (GroupItem item : local.queryAllGroups()) {
			if (p.matcher((String)item.getGroup().getValue()).find()) {
			    items.add(JOVALSystem.factories.sc.windows.createGroupItem(item));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		break;
    
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", gObj.getGroup().getOperation()));
	    }
	} catch (WmiException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage("ERROR_WINWMI_GENERAL", e.getMessage()));
	    rc.addMessage(msg);
	}
	if (items.size() == 0) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALSystem.getMessage("STATUS_NOT_FOUND", group, gObj.getId()));
	    rc.addMessage(msg);
	}
	return items;
    }
}
