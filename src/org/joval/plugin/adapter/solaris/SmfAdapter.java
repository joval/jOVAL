// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.solaris.SmfObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfProtocolType;
import oval.schemas.systemcharacteristics.solaris.EntityItemSmfServiceStateType;
import oval.schemas.systemcharacteristics.solaris.SmfItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
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
    private ISession session;
    private String[] services = null;
    private Hashtable<String, SmfItem> serviceMap = null;

    public SmfAdapter(ISession session) {
	this.session = session;
	serviceMap = new Hashtable<String, SmfItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return SmfObject.class;
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
		    if (line.startsWith("FMRI")) {
			continue;
		    }
		    String fmri = getFullFmri(line.trim());
		    if (fmri == null) {
			JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FMRI", line));
		    } else {
			list.add(fmri);
		    }
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

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	SmfObject sObj = (SmfObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

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
		rc.addMessage(msg);
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
	    try {
		Pattern p = Pattern.compile((String)sObj.getFmri().getValue());
		for (String fmri : serviceMap.keySet()) {
		    if (p.matcher(fmri).find()) {
			items.add(JOVALSystem.factories.sc.solaris.createSmfItem(serviceMap.get(fmri)));
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
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", sObj.getFmri().getOperation()));
	}
	return items;
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

    /**
     * DAS: need to add protocol, serverArguments, serverExecutable and execAsUser types...
     */
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
	    if (ptr == -1) {
		return null;
	    }
	    StringBuffer sb = new StringBuffer(fmri.substring(0, ptr));
	    sb.append("//localhost");
	    sb.append(fmri.substring(ptr));
	    return sb.toString();
	} else {
	    return fmri;
	}
    }
}
