// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.linux.RpminfoObject;
import oval.schemas.definitions.linux.RpminfoState;
import oval.schemas.definitions.linux.RpminfoTest;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.linux.ObjectFactory;
import oval.schemas.systemcharacteristics.linux.RpminfoItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter {
    private IAdapterContext ctx;
    private ISession session;
    private ObjectFactory linuxFactory;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;

    public RpminfoAdapter(ISession session) {
	this.session = session;
	linuxFactory = new ObjectFactory();
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return RpminfoObject.class;
    }

    public Class getStateClass() {
	return RpminfoState.class;
    }

    public Class getItemClass() {
	return RpminfoItem.class;
    }

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    items.add(linuxFactory.createRpminfoItem(getItem((RpminfoObject)obj)));
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
	RpminfoState state = (RpminfoState)st;
	RpminfoItem item = (RpminfoItem)it;

	if (state.getEvr() == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_BAD", state.getId()));
	} else {
	    String evr = (String)state.getEvr().getValue();
	    int end = evr.indexOf(":");
	    String epoch = evr.substring(0, end);
	    int begin = end+1;
	    end = evr.indexOf("-", begin);
	    String version = evr.substring(begin, end);
	    String release = evr.substring(end+1);
    
	    switch(state.getEvr().getOperation()) {
	      case EQUALS:
		if (epoch.equals((String)item.getEpoch().getValue()) &&
		    version.equals((String)item.getVersion().getValue()) &&
		    release.equals((String)item.getRelease().getValue())) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}

	      case LESS_THAN:
		if (Version.isVersion(epoch) && Version.isVersion((String)item.getEpoch().getValue())) {
		    Version stateEpoch = new Version(epoch);
		    Version itemEpoch = new Version((String)item.getEpoch().getValue());
		    if (itemEpoch.lessThan(stateEpoch)) {
			return ResultEnumeration.TRUE;
		    }
		} else if (((String)item.getEpoch().getValue()).compareTo(epoch) > 0) {
		    return ResultEnumeration.TRUE;
		}
		if (Version.isVersion(version) && Version.isVersion((String)item.getVersion().getValue())) {
		    Version stateEpoch = new Version(version);
		    Version itemEpoch = new Version((String)item.getVersion().getValue());
		    if (itemEpoch.lessThan(stateEpoch)) {
			return ResultEnumeration.TRUE;
		    }
		} else if (((String)item.getVersion().getValue()).compareTo(version) > 0) {
		    return ResultEnumeration.TRUE;
		}
		if (Version.isVersion(release) && Version.isVersion((String)item.getRelease().getValue())) {
		    Version stateEpoch = new Version(release);
		    Version itemEpoch = new Version((String)item.getRelease().getValue());
		    if (itemEpoch.lessThan(stateEpoch)) {
			return ResultEnumeration.TRUE;
		    }
		} else if (((String)item.getRelease().getValue()).compareTo(release) > 0) {
		    return ResultEnumeration.TRUE;
		}
		return ResultEnumeration.FALSE;

	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
							       state.getEvr().getOperation()));
	    }
	}
    }

    // Private

    private RpminfoItem getItem(RpminfoObject obj) throws Exception {
	RpminfoItem item = linuxFactory.createRpminfoItem();

	String packageName = (String)obj.getName().getValue();
	IProcess p = session.createProcess("rpm -q " + packageName);
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();

	if (result.indexOf("not installed") == -1) {
	    p = session.createProcess("rpm -q --qf '%{EPOCH}\\n' " + packageName);
	    p.start();
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String pkgEpoch = br.readLine();
	    br.close();
	    if (pkgEpoch.equals("(none)")) {
		pkgEpoch = "0";
	    }

	    int end = result.indexOf("-");
	    String pkgName = result.substring(0, end);
	    int begin = end+1;
	    end = result.indexOf("-", begin+1);
	    String pkgRelease = result.substring(begin, end);
	    begin = end+1;
	    end = result.lastIndexOf(".");
	    String pkgVersion = result.substring(begin, end);
	    String pkgArch = result.substring(end+1);

	    item.setStatus(StatusEnumeration.EXISTS);
	    EntityItemStringType name = coreFactory.createEntityItemStringType();
	    name.setValue(pkgName);
	    item.setName(name);

	    RpminfoItem.Epoch epoch = linuxFactory.createRpminfoItemEpoch();
	    epoch.setValue(pkgEpoch);
	    item.setEpoch(epoch);

	    EntityItemStringType arch = coreFactory.createEntityItemStringType();
	    arch.setValue(pkgArch);
	    item.setArch(arch);

	    RpminfoItem.Version version = linuxFactory.createRpminfoItemVersion();
	    version.setValue(pkgVersion);
	    item.setVersion(version);

	    RpminfoItem.Release release = linuxFactory.createRpminfoItemRelease();
	    release.setValue(pkgRelease);
	    item.setRelease(release);

	    EntityItemEVRStringType evr = new EntityItemEVRStringType();
	    evr.setValue(pkgEpoch + ":" + pkgVersion + "-" + pkgRelease);
	    evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
	    item.setEvr(evr);
	} else {
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}

	return item;
    }
}
