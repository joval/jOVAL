// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.xml.bind.JAXBElement;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectIntType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.linux.SelinuxsecuritycontextObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.linux.SelinuxsecuritycontextItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Retrieves items for Selinuxsecuritycontext objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SelinuxsecuritycontextAdapter extends BaseFileAdapter<SelinuxsecuritycontextItem> {
    private IUnixSession us;
    private Map<Integer, SelinuxsecuritycontextItem> processMap;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	baseInit(session);
	us = (IUnixSession)session;
	processMap = new HashMap<Integer, SelinuxsecuritycontextItem>();
	Collection<Class> classes = new ArrayList<Class>();
	classes.add(SelinuxsecuritycontextObject.class);
	return classes;
    }

    // Implement IProvider

    @Override
    public Collection<SelinuxsecuritycontextItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	SelinuxsecuritycontextObject sObj = (SelinuxsecuritycontextObject)obj;
	if (sObj.isSetPid()) {
	    return getItems(sObj.getPid(), rc);
	} else {
	    return super.getItems(obj, rc);
	}
    }

    // Protected

    protected Class getItemClass() {
	return SelinuxsecuritycontextItem.class;
    }

    /**
     * Entry point for the BaseFileAdapter super-class.
     */
    protected Collection<SelinuxsecuritycontextItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	Collection<SelinuxsecuritycontextItem> items = new ArrayList<SelinuxsecuritycontextItem>();
	for (IFile f : files) {
	    try {
		SelinuxsecuritycontextItem baseItem = (SelinuxsecuritycontextItem)getBaseItem(obj, f);
		if (f.getExtended() instanceof IUnixFileInfo) {
	            IUnixFileInfo info = (IUnixFileInfo)f.getExtended();
		    parseSecurityContextData(baseItem, info.getExtendedData(IUnixFileInfo.SELINUX_DATA));
		    items.add(baseItem);
	        }
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    // Private

    private Collection<SelinuxsecuritycontextItem> getItems(JAXBElement<EntityObjectIntType> elt, IRequestContext rc)
		throws CollectException {

	Collection<SelinuxsecuritycontextItem> items = new ArrayList<SelinuxsecuritycontextItem>();
	EntityObjectIntType pidType = elt.getValue();
	if (pidType == null || pidType.getOperation() == OperationEnumeration.EQUALS) {
	    try {
		return Arrays.asList(getItem(elt));
	    } catch (NoSuchElementException e) {
		// no match
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	} else {
	    Integer iPid = new Integer((String)pidType.getValue());
	    OperationEnumeration op = pidType.getOperation();
	    loadProcesses();
	    switch(op) {
	      case NOT_EQUAL:
	      case GREATER_THAN:
	      case GREATER_THAN_OR_EQUAL:
	      case LESS_THAN:
	      case LESS_THAN_OR_EQUAL:
		for (Integer i : processMap.keySet()) {
		    switch(op) {
		      case NOT_EQUAL:
			if (i.compareTo(iPid) != 0) {
			    items.add(processMap.get(i));
			}
			break;
		      case LESS_THAN:
			if (i.compareTo(iPid) < 0) {
			    items.add(processMap.get(i));
			}
			break;
		      case LESS_THAN_OR_EQUAL:
			if (i.compareTo(iPid) <= 0) {
			    items.add(processMap.get(i));
			}
			break;
		      case GREATER_THAN:
			if (i.compareTo(iPid) > 0) {
			    items.add(processMap.get(i));
			}
			break;
		      case GREATER_THAN_OR_EQUAL:
			if (i.compareTo(iPid) >= 0) {
			    items.add(processMap.get(i));
			}
			break;
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	return items;
    }

    /**
     * Follows the OpenSCAP implementation, which doesn't distinguish between raw-hi/lo and hi/lo either.
     */
    private void parseSecurityContextData(SelinuxsecuritycontextItem item, String context) {
	StringTokenizer tok = new StringTokenizer(context, ":");
	if (tok.countTokens() > 2) {
	    EntityItemStringType user = Factories.sc.core.createEntityItemStringType();
	    user.setValue(tok.nextToken());
	    item.setUser(user);

	    EntityItemStringType role = Factories.sc.core.createEntityItemStringType();
	    role.setValue(tok.nextToken());
	    item.setRole(role);

	    EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
	    type.setValue(tok.nextToken());
	    item.setType(type);

	    if (tok.hasMoreTokens()) {
		String level = tok.nextToken("\n").substring(1); // rest of entry data, less beginning :
		String loRange=null, hiRange=null;
		String[] sa = split(level, "-");
		if (sa.length == 1) {
		    loRange = sa[0].trim();
		} else {
		    loRange = sa[0].trim();
		    hiRange = sa[1].trim();
		}

		String loSen=null, loCat=null;
		sa = split(loRange, ":");
		if (sa.length == 1) {
		    loSen = sa[0].trim();
		} else {
		    loSen = sa[0].trim();
		    loCat = sa[1].trim();
		}
		EntityItemStringType lowSensitivity = Factories.sc.core.createEntityItemStringType();
		lowSensitivity.setValue(loSen);
		item.setLowSensitivity(lowSensitivity);
		item.setRawlowSensitivity(lowSensitivity);
		if (loCat != null) {
		    EntityItemStringType lowCategoryType = Factories.sc.core.createEntityItemStringType();
		    lowCategoryType.setValue(loCat);
		    item.setLowCategory(lowCategoryType);
		    item.setRawlowCategory(lowCategoryType);
		}

		if (hiRange != null) {
		    String hiSen=null, hiCat=null;
		    sa = split(hiRange, ":");
		    if (sa.length == 1) {
			hiSen = sa[0].trim();
		    } else {
			hiSen = sa[0].trim();
			hiCat = sa[1].trim();
		    }
		    EntityItemStringType highSensitivity = Factories.sc.core.createEntityItemStringType();
		    highSensitivity.setValue(hiSen);
		    item.setHighSensitivity(highSensitivity);
		    item.setRawhighSensitivity(highSensitivity);
		    if (hiCat != null) {
			EntityItemStringType highCategory = Factories.sc.core.createEntityItemStringType();
			highCategory.setValue(hiCat);
			item.setHighCategory(highCategory);
			item.setRawhighCategory(highCategory);
		    }
		}
	    }
	}
    }

    private SelinuxsecuritycontextItem getItem(JAXBElement<EntityObjectIntType> elt) throws Exception {
	if (elt.getValue() == null || ((String)elt.getValue().getValue()).length() == 0) {
	    //
	    // According to the spec, xsi:nil means get the context of the "current process" -- which for us is the login.
	    //
	    SelinuxsecuritycontextItem item = Factories.sc.linux.createSelinuxsecuritycontextItem();
	    parseSecurityContextData(item, SafeCLI.exec("id -Z", us, IUnixSession.Timeout.S));
	    return item;
	} else {
	    Integer iPid = new Integer((String)elt.getValue().getValue());
	    if (processMap.containsKey(iPid)) {
		return processMap.get(iPid);
	    } else {
		String cmd = new StringBuffer("ps -Z --no-headers ").append(iPid.toString()).toString();
		SelinuxsecuritycontextItem item = parseProcess(SafeCLI.exec(cmd, us, IUnixSession.Timeout.S));
		if (item == null) {
		    throw new NoSuchElementException(iPid.toString());
		} else {
		    processMap.put(new Integer((String)item.getPid().getValue()), item);
		    return item;
		}
	    }
	}
    }

    private SelinuxsecuritycontextItem parseProcess(String line) throws IllegalArgumentException {
	StringTokenizer tok = new StringTokenizer(line);
	if (tok.countTokens() > 1) {
	    String selinux = tok.nextToken();
	    String pid = tok.nextToken();
	    Integer.parseInt(pid);

	    SelinuxsecuritycontextItem item = Factories.sc.linux.createSelinuxsecuritycontextItem();
	    EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
	    pidType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    pidType.setValue(pid);
	    item.setPid(pidType);

	    parseSecurityContextData(item, selinux);
	    return item;
	} else {
	    throw new IllegalArgumentException(line);
	}
    }

    private boolean loaded = false;
    private void loadProcesses() {
	if (!loaded) {
	    try {
		for (String line : SafeCLI.multiLine("ps -eZ --no-headers", us, IUnixSession.Timeout.S)) {
		    if (line.length() > 0) {
			try {
			    SelinuxsecuritycontextItem item = parseProcess(line);
			    processMap.put(new Integer((String)item.getPid().getValue()), item);
			} catch (IllegalArgumentException e) {
			    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_SELINUX_SC, e.getMessage()));
			    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			}
		    }
		}
		loaded = true;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    private String[] split(String str, String delim) {
	int ptr = str.indexOf(delim);
	if (ptr > 0) {
	    int delimLen = delim.length();
	    String lhs = str.substring(0,ptr);
	    String rhs = str.substring(ptr+delimLen);
	    String[] sa = {lhs, rhs};
	    return sa;
	} else {
	    String[] sa = {str};
	    return sa;
	}
    }
}
