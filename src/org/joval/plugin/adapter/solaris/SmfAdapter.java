// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
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
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfProtocolType;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfServiceStateType;
import oval.schemas.systemcharacteristics.solaris.SmfItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
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
    private String[] services = null;
    private Hashtable<String, SmfItem> serviceMap = null;

    public SmfAdapter(ISession session) {
	this.session = session;
	serviceMap = new Hashtable<String, SmfItem>();
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
	if (session != null) {
	    BufferedReader br = null;
	    try {
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_SMF"));
		IProcess p = session.createProcess("/usr/bin/svcs -o fmri");
		p.start();
		br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		ArrayList<String> list = new ArrayList<String>();
		String line = null;
		while((line = br.readLine()) != null) {
		    if (line.startsWith("FRMI")) {
			continue;
		    }
		    list.add(getFullFmri(line.trim()));
		}
		services = list.toArray(new String[list.size()]);
	    } catch (Exception e) {
		JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	    } finally {
		if (br != null) {
		    try {
			br.close();
		    } catch (IOException e) {
		    }
		}
	    }
	}
	return services != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	SmfObject sObj = (SmfObject)obj;
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	switch(sObj.getFmri().getOperation()) {
	  case EQUALS:
	    try {
		SmfItem item = getItem((String)sObj.getFmri().getValue());
		if (item != null) {
		    items.add(JOVALSystem.factories.sc.solaris.createSmfItem(item));
		}
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		ctx.addObjectMessage(obj.getId(), msg);
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadFullServiceMap();
	    for (String fmri : serviceMap.keySet()) {
		if (!fmri.equals((String)sObj.getFmri().getValue())) {
		    items.add(JOVALSystem.factories.sc.solaris.createSmfItem(serviceMap.get(fmri)));
		}
	    }
	    break;
	  }

	  case PATTERN_MATCH: {
	    loadFullServiceMap();
	    Pattern p = Pattern.compile((String)sObj.getFmri().getValue());
	    for (String fmri : serviceMap.keySet()) {
		if (p.matcher(fmri).find()) {
		    items.add(JOVALSystem.factories.sc.solaris.createSmfItem(serviceMap.get(fmri)));
		}
	    }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", sObj.getFmri().getOperation()));
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	SmfState state = (SmfState)st;
	SmfItem item = (SmfItem)it;

	if (state.isSetFmri()) {
	    ResultEnumeration result = ctx.test(state.getFmri(), item.getFmri());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetServiceName()) {
	    ResultEnumeration result = ctx.test(state.getServiceName(), item.getServiceName());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetServiceState()) {
	    ResultEnumeration result = ctx.test(state.getServiceState(), item.getServiceState());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	return ResultEnumeration.TRUE;
    }

    // Internal

    private boolean loaded = false;
    private void loadFullServiceMap() {
	if (loaded) return;

	serviceMap = new Hashtable<String, SmfItem>();
	for (int i=0; i < services.length; i++) {
	    try {
		SmfItem item = getItem(services[i]);
		serviceMap.put((String)item.getFmri().getValue(), item);
	    } catch (Exception e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_SMF", services[i], e.getMessage()), e);
	    }
	}
	loaded = true;
    }

    private static final String FMRI		= "fmri";
    private static final String NAME		= "name";
    private static final String ENABLED		= "enabled";
    private static final String STATE		= "state";
    private static final String NEXT_STATE	= "next_state";
    private static final String STATE_TIME	= "state_time";
    private static final String RESTARTER	= "restarter";
    private static final String DEPENDENCY	= "dependency";

    private SmfItem getItem(String fmri) throws Exception {
	SmfItem item = serviceMap.get(fmri);
	if (item != null) {
	    return item;
	}

	JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_SMF_SERVICE", fmri));
	item = JOVALSystem.factories.sc.solaris.createSmfItem();
	IProcess p = session.createProcess("/usr/bin/svcs -l " + fmri);
	p.start();
	BufferedReader br = null;
	boolean found = false;
	try {
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = null;
	    while((line = br.readLine()) != null) {
		line = line.trim();
		if (line.startsWith(FMRI)) {
		    found = true;
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(getFullFmri(line.substring(FMRI.length()).trim()));
		    item.setFmri(type);
		} else if (line.startsWith(NAME)) {
		    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    type.setValue(line.substring(NAME.length()).trim());
		    item.setServiceName(type);
		} else if (line.startsWith(STATE_TIME)) { // NB: this MUST appear before STATE
		} else if (line.startsWith(STATE)) {
		    EntityItemSmfServiceStateType type = JOVALSystem.factories.sc.solaris.createEntityItemSmfServiceStateType();
		    type.setValue(line.substring(STATE.length()).trim().toUpperCase());
		    item.setServiceState(type);
		}
	    }
	} finally {
	    if (br != null) {
		br.close();
	    }
	}

	if (found) {
	    item.setStatus(StatusEnumeration.EXISTS);
	} else {
	    EntityItemStringType fmriType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    fmriType.setValue(fmri);
	    item.setFmri(fmriType);
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}

	return item;
    }

    /**
     * Generally, prepend "localhost".
     */
    private String getFullFmri(String fmri) {
	if (fmri.indexOf("//") == -1) {
	    int ptr = fmri.indexOf("/");
	    StringBuffer sb = new StringBuffer(fmri.substring(0, ptr));
	    sb.append("//localhost");
	    sb.append(fmri.substring(ptr));
	    return sb.toString();
	} else {
	    return fmri;
	}
    }
}
