// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.OperatorEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.PackageObject;
import oval.schemas.definitions.solaris.PackageState;
import oval.schemas.definitions.solaris.PackageTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.solaris.PackageItem;
import oval.schemas.systemcharacteristics.solaris.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.TypeTools;

/**
 * Evaluates the Solaris Package OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private IAdapterContext ctx;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory solarisFactory;

    public PackageAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	solarisFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return PackageObject.class;
    }

    public Class getStateClass() {
	return PackageState.class;
    }

    public Class getItemClass() {
	return PackageItem.class;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	try {
	    PackageItem item = getItem((PackageObject)obj);
	    if (item != null) {
		items.add(solarisFactory.createPackageItem(item));
	    }
	} catch (Exception e) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    ctx.addObjectMessage(obj.getId(), msg);
	    ctx.log(Level.WARNING, e.getMessage(), e);
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	if (compare((PackageState)st, (PackageItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Internal

    private boolean compare(PackageState state, PackageItem item) throws OvalException {
	switch(state.getOperator()) {
	  case OR:
	    if (state.isSetCategory()) {
		if (item.isSetCategory()) {
		    return TypeTools.compare(state.getCategory(), item.getCategory());
		} else {
		    return false;
		}
	    } else if (state.isSetDescription()) {
		if (item.isSetDescription()) {
		    return TypeTools.compare(state.getDescription(), item.getDescription());
		} else {
		    return false;
		}
	    } else if (state.isSetName()) {
		if (item.isSetName()) {
		    return TypeTools.compare(state.getName(), item.getName());
		} else {
		    return false;
		}
	    } else if (state.isSetPackageVersion()) {
		if (item.isSetVersion()) {
		    return TypeTools.compare(state.getPackageVersion(), item.getVersion());
		} else {
		    return false;
		}
	    } else if (state.isSetPkginst()) {
		if (item.isSetPkginst()) {
		    return TypeTools.compare(state.getPkginst(), item.getPkginst());
		} else {
		    return false;
		}
	    } else if (state.isSetVendor()) {
		if (item.isSetVendor()) {
		    return TypeTools.compare(state.getVendor(), item.getVendor());
		} else {
		    return false;
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
	    }

	  case AND:
	    if (state.isSetCategory()) {
		if (item.isSetCategory()) {
		   if (!TypeTools.compare(state.getCategory(), item.getCategory())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    if (state.isSetDescription()) {
		if (item.isSetDescription()) {
		    if (!TypeTools.compare(state.getDescription(), item.getDescription())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    if (state.isSetName()) {
		if (item.isSetName()) {
		    if (!TypeTools.compare(state.getName(), item.getName())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    if (state.isSetPackageVersion()) {
		if (item.isSetVersion()) {
		    if (!TypeTools.compare(state.getPackageVersion(), item.getVersion())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    if (state.isSetPkginst()) {
		if (item.isSetPkginst()) {
		    if (!TypeTools.compare(state.getPkginst(), item.getPkginst())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    if (state.isSetVendor()) {
		if (item.isSetVendor()) {
		    if (!TypeTools.compare(state.getVendor(), item.getVendor())) {
			return false;
		    }
		} else {
		    return false;
		}
	    }
	    return true;

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATOR", state.getOperator()));
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

    private PackageItem getItem(PackageObject obj) throws Exception {
	PackageItem item = solarisFactory.createPackageItem();

	String pkginst = (String)obj.getPkginst().getValue();
	IProcess p = session.createProcess("/usr/bin/pkginfo -l " + pkginst);
	p.start();
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = null;
	    boolean found = false;
	    while((line = br.readLine()) != null) {
		line = line.trim();
		if (line.startsWith(PKGINST)) {
		    found = true;
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(PKGINST.length()).trim());
		    item.setPkginst(type);
		} else if (line.startsWith(NAME)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(NAME.length()).trim());
		    item.setName(type);
		} else if (line.startsWith(DESC)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(DESC.length()).trim());
		    item.setDescription(type);
		} else if (line.startsWith(CATEGORY)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(CATEGORY.length()).trim());
		    item.setCategory(type);
		} else if (line.startsWith(VENDOR)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(VENDOR.length()).trim());
		    item.setVendor(type);
		} else if (line.startsWith(VERSION)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(VERSION.length()).trim());
		    item.setVersion(type);
		}
	    }
	    if (!found) {
		return null;
	    }
	} finally {
	    if (br != null) {
		br.close();
	    }
	}

	return item;
    }
}
