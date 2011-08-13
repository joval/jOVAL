// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.PackageObject;
import oval.schemas.definitions.solaris.PackageState;
import oval.schemas.definitions.solaris.PackageTest;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.solaris.PackageItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Package OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private IAdapterContext ctx;
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

    public Class getStateClass() {
	return PackageState.class;
    }

    public Class getItemClass() {
	return PackageItem.class;
    }

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public boolean connect() {
	if (session != null) {
	    BufferedReader br = null;
	    try {
		ArrayList<String> list = new ArrayList<String>();
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_SOLPKG_LIST"));
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
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
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

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	PackageObject pObj = (PackageObject)obj;
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(pObj.getPkginst().getOperation()) {
	  case EQUALS:
	    try {
		items.add(JOVALSystem.factories.sc.solaris.createPackageItem(getItem((String)pObj.getPkginst().getValue())));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage("ERROR_SOLPKG", (String)pObj.getPkginst().getValue(), e.getMessage());
		msg.setValue(s);
		ctx.addObjectMessage(obj.getId(), msg);
		ctx.log(Level.WARNING, s, e);
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
		msg.setValue(e.getMessage());
		ctx.addObjectMessage(obj.getId(), msg);
		ctx.log(Level.WARNING, e.getMessage(), e);
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

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", pObj.getPkginst().getOperation()));
	}

	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	PackageState state = (PackageState)st;
	PackageItem item = (PackageItem)it;

	if (state.isSetCategory()) {
	    ResultEnumeration result = ctx.test(state.getCategory(), item.getCategory());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetDescription()) {
	    ResultEnumeration result = ctx.test(state.getDescription(), item.getDescription());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetName()) {
	    ResultEnumeration result = ctx.test(state.getName(), item.getName());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetPackageVersion()) {
	    ResultEnumeration result = ctx.test(state.getPackageVersion(), item.getVersion());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetPkginst()) {
	    ResultEnumeration result = ctx.test(state.getPkginst(), item.getPkginst());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetVendor()) {
	    ResultEnumeration result = ctx.test(state.getVendor(), item.getVendor());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	return ResultEnumeration.TRUE;
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
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_PKGINFO", packages[i], e.getMessage()), e);
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

	JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_SOLPKG_PKGINFO", pkginst));
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
		    item.setVersion(type);
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
