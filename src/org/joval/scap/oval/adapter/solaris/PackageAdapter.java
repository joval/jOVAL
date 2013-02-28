// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.PackageObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.solaris.PackageItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Package OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, PackageItem> packageMap;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    this.session = (IUnixSession)session;
	    packageMap = new HashMap<String, PackageItem>();
	    classes.add(PackageObject.class);
	} else {
	    notapplicable.add(PackageObject.class);
	}
	return classes;
    }

    public Collection<PackageItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	PackageObject pObj = (PackageObject)obj;
	Collection<PackageItem> items = new ArrayList<PackageItem>();
	switch(pObj.getPkginst().getOperation()) {
	  case EQUALS:
	    try {
		String pkginst = SafeCLI.checkArgument((String)pObj.getPkginst().getValue(), session);
		if (packageMap.containsKey(pkginst)) {
		    items.add(packageMap.get(pkginst));
		} else {
		    String cmd = "pkginfo -l '" + pkginst + "'";
		    PackageItem item = nextPackageItem(SafeCLI.multiLine(cmd, session, IUnixSession.Timeout.M).iterator());
		    if (item == null) {
			item = Factories.sc.solaris.createPackageItem();
			EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
			type.setValue(pkginst);
			item.setPkginst(type);
			item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		    }
		    packageMap.put(pkginst, item);
		    items.add(item);
		}
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadFullPackageMap();
	    try {
		Pattern p = Pattern.compile((String)pObj.getPkginst().getValue());
		for (String pkginst : packageMap.keySet()) {
		    if (p.matcher(pkginst).find()) {
			items.add(packageMap.get(pkginst));
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
	    loadFullPackageMap();
	    String pkginst = (String)pObj.getPkginst().getValue();
	    for (String key : packageMap.keySet()) {
		if (!pkginst.equals(key)) {
		    items.add(packageMap.get(key));
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getPkginst().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	String cmd = "pkginfo | awk '{print $2}' | xargs -I{} pkginfo -l '{}'";
	try {
	    Iterator<String> lines = SafeCLI.manyLines(cmd, null, session);
	    PackageItem item = null;
	    while((item = nextPackageItem(lines)) != null) {
		packageMap.put((String)item.getPkginst().getValue(), item);
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static final String PKGINST		= "PKGINST:";
    private static final String NAME		= "NAME:";
    private static final String CATEGORY	= "CATEGORY:";
    private static final String ARCH		= "ARCH:";
    private static final String VERSION		= "VERSION:";
    private static final String BASEDIR		= "BASEDIR:";
    private static final String VENDOR		= "VENDOR:";
    private static final String DESC		= "DESC:";
    private static final String PSTAMP		= "PSTAMP:";
    private static final String INSTDATE	= "INSTDATE:";
    private static final String HOTLINE		= "HOTLINE:";
    private static final String STATUS		= "STATUS:";
    private static final String FILES		= "FILES:";
    private static final String ERROR		= "ERROR:";

    private PackageItem nextPackageItem(Iterator<String> lines) {
	PackageItem item = null;
	while(lines.hasNext()) {
	    String line = lines.next().trim();
	    if (line.length() == 0) {
		break;
	    } else if (line.startsWith(PKGINST)) {
		item = Factories.sc.solaris.createPackageItem();
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		String pkginst = line.substring(PKGINST.length()).trim();
		type.setValue(pkginst);
		session.getLogger().debug(JOVALMsg.STATUS_SOLPKG_PKGINFO, pkginst);
		item.setPkginst(type);
	    } else if (line.startsWith(NAME)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(NAME.length()).trim());
		item.setName(type);
	    } else if (line.startsWith(DESC)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(DESC.length()).trim());
		item.setDescription(type);
	    } else if (line.startsWith(CATEGORY)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(CATEGORY.length()).trim());
		item.setCategory(type);
	    } else if (line.startsWith(VENDOR)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VENDOR.length()).trim());
		item.setVendor(type);
	    } else if (line.startsWith(VERSION)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VERSION.length()).trim());
		item.setPackageVersion(type);
	    }
	}
	return item;
    }
}
