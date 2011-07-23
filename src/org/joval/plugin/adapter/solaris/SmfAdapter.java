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
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.OperatorEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateSimpleBaseType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.EntityStateSmfProtocolType;
import oval.schemas.definitions.solaris.EntityStateSmfServiceStateType;
import oval.schemas.definitions.solaris.SmfObject;
import oval.schemas.definitions.solaris.SmfState;
import oval.schemas.definitions.solaris.SmfTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfProtocolType;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfServiceStateType;
import oval.schemas.systemcharacteristics.solaris.ObjectFactory;
import oval.schemas.systemcharacteristics.solaris.SmfItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates the Solaris SMF OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmfAdapter implements IAdapter {
    private IAdapterContext ctx;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory solarisFactory;

    public SmfAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	solarisFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return SmfObject.class;
    }

    public Class getStateClass() {
	return SmfState.class;
    }

    public Class getItemClass() {
	return SmfItem.class;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    SmfItem item = getItem((SmfObject)obj);
	    if (item != null) {
		items.add(solarisFactory.createSmfItem(item));
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
	if (compare((SmfState)st, (SmfItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Internal

    private boolean compare(SmfState state, SmfItem item) throws OvalException {
	if (state.isSetFmri()) {
	    if (item.isSetFmri()) {
		return compare(state.getFmri(), item.getFmri());
	    } else {
		return false;
	    }
	} else if (state.isSetServiceName()) {
	    if (item.isSetServiceName()) {
		return compare(state.getServiceName(), item.getServiceName());
	    } else {
		return false;
	    }
	} else if (state.isSetServiceState()) {
	    if (item.isSetServiceState()) {
		return compare(state.getServiceState(), item.getServiceState());
	    } else {
		return false;
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
	}
    }

    private boolean compare(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item) throws OvalException {
	switch(state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    return ((String)state.getValue()).equalsIgnoreCase((String)item.getValue());
	  case EQUALS:
	    return ((String)state.getValue()).equals((String)item.getValue());
	  case PATTERN_MATCH:
	    if (item.getValue() == null) {
		return false;
	    } else {
		return Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find();
	    }
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

    private static final String FMRI		= "fmri";
    private static final String NAME		= "name";
    private static final String ENABLED		= "enabled";
    private static final String STATE		= "state";
    private static final String NEXT_STATE	= "next_state";
    private static final String STATE_TIME	= "state_time";
    private static final String RESTARTER	= "restarter";
    private static final String DEPENDENCY	= "dependency";

    private SmfItem getItem(SmfObject obj) throws Exception {
	SmfItem item = solarisFactory.createSmfItem();

	String fmri = (String)obj.getFmri().getValue();
	IProcess p = session.createProcess("/usr/bin/svcs -l " + fmri);
	p.start();
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = null;
	    boolean found = false;
	    while((line = br.readLine()) != null) {
		line = line.trim();
		if (line.startsWith(FMRI)) {
		    found = true;
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(FMRI.length()).trim());
		    item.setFmri(type);
		} else if (line.startsWith(NAME)) {
		    EntityItemStringType type = coreFactory.createEntityItemStringType();
		    type.setValue(line.substring(NAME.length()).trim());
		    item.setServiceName(type);
		} else if (line.startsWith(STATE_TIME)) { // NB: this MUST appear before STATE
		} else if (line.startsWith(STATE)) {
		    EntityItemSmfServiceStateType type = solarisFactory.createEntityItemSmfServiceStateType();
		    type.setValue(line.substring(STATE.length()).trim().toUpperCase());
		    item.setServiceState(type);
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
