// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.solaris.PackageObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.solaris.PackageItem;

import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Package OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private ISession session;
    private Hashtable<String, PackageItem> packageMap;
    private String[] packages;

    public PackageAdapter(ISession session) {
	this.session = session;
	packageMap = new Hashtable<String, PackageItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return PackageObject.class;
    }

    public boolean connect() {
	if (session != null) {
	    BufferedReader br = null;
	    try {
		ArrayList<String> list = new ArrayList<String>();
		JOVALSystem.getLogger().trace(JOVALMsg.STATUS_SOLPKG_LIST);
		IProcess p = session.createProcess("pkginfo -x");
		p.start();
		br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null) {
		    switch(line.charAt(0)) {
		      case ' ':
		      case '\t':
			break;

		      default:
			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() > 0) {
			    list.add(tok.nextToken());
			}
			break;
		    }
		}
		packages = list.toArray(new String[list.size()]);
		return true;
	    } catch (Exception e) {
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		if (br != null) {
		    try {
			br.close();
		    } catch (IOException e) {
		    }
		}
	    }
	}
	return false;
    }

    public void disconnect() {
	packageMap = null;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	PackageObject pObj = (PackageObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(pObj.getPkginst().getOperation()) {
	  case EQUALS:
	    try {
		items.add(JOVALSystem.factories.sc.solaris.createPackageItem(getItem((String)pObj.getPkginst().getValue())));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_SOLPKG, (String)pObj.getPkginst().getValue(), e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadFullPackageMap();
	    try {
		Pattern p = Pattern.compile((String)pObj.getPkginst().getValue());
		for (String pkginst : packageMap.keySet()) {
		    if (p.matcher(pkginst).find()) {
			items.add(JOVALSystem.factories.sc.solaris.createPackageItem(packageMap.get(pkginst)));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadFullPackageMap();
	    String pkginst = (String)pObj.getPkginst().getValue();
	    for (String key : packageMap.keySet()) {
		if (!pkginst.equals(key)) {
		    items.add(JOVALSystem.factories.sc.solaris.createPackageItem(packageMap.get(key)));
		}
	    }
	    break;
	  }

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getPkginst().getOperation());
	    throw new OvalException(s);
	  }
	}

	return items;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	packageMap = new Hashtable<String, PackageItem>();
	for (int i=0; i < packages.length; i++) {
	    try {
		PackageItem item = getItem(packages[i]);
		packageMap.put((String)item.getPkginst().getValue(), item);
	    } catch (Exception e) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_SOLPKG_PKGINFO, packages[i]);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	loaded = true;
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

    private PackageItem getItem(String pkginst) throws Exception {
	PackageItem item = packageMap.get(pkginst);
	if (item != null) {
	    return item;
	}

	JOVALSystem.getLogger().trace(JOVALMsg.STATUS_SOLPKG_PKGINFO, pkginst);
	item = JOVALSystem.factories.sc.solaris.createPackageItem();
	IProcess p = session.createProcess("/usr/bin/pkginfo -l " + pkginst);
	p.start();
	BufferedReader br = null;
	boolean isInstalled = false;
	try {
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = null;
	    while((line = br.readLine()) != null) {
		line = line.trim();
		if (line.startsWith(PKGINST)) {
		    isInstalled = true;
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(PKGINST.length()).trim());
		    item.setPkginst(type);
		} else if (line.startsWith(NAME)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(NAME.length()).trim());
		    item.setName(type);
		} else if (line.startsWith(DESC)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(DESC.length()).trim());
		    item.setDescription(type);
		} else if (line.startsWith(CATEGORY)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(CATEGORY.length()).trim());
		    item.setCategory(type);
		} else if (line.startsWith(VENDOR)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(VENDOR.length()).trim());
		    item.setVendor(type);
		} else if (line.startsWith(VERSION)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(VERSION.length()).trim());
		    item.setPackageVersion(type);
		}
	    }
	} finally {
	    if (br != null) {
		br.close();
	    }
	}

	if (isInstalled) {
	    item.setStatus(StatusEnumeration.EXISTS);
	} else {
	    EntityItemStringType pkginstType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    pkginstType.setValue(pkginst);
	    item.setPkginst(pkginstType);
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}

	packageMap.put(pkginst, item);
	return item;
    }
}
