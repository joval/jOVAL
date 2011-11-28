// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.unix.UnameObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.unix.UnameItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UnameTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnameAdapter implements IAdapter {
    private IUnixSession session;

    public UnameAdapter(IUnixSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {UnameObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectionException, OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    items.add(getItem());
	} catch (Exception e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Internal

    private JAXBElement<UnameItem> getItem() throws Exception {
	UnameItem item = JOVALSystem.factories.sc.unix.createUnameItem();
	EntityItemStringType machineClass = JOVALSystem.factories.sc.core.createEntityItemStringType();
	IProcess p = session.createProcess("uname -m", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();
	p.waitFor(0);
	machineClass.setValue(result);
	item.setMachineClass(machineClass);

	EntityItemStringType nodeName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("uname -n", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	nodeName.setValue(result);
	item.setNodeName(nodeName);

	EntityItemStringType osName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("uname -s", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	osName.setValue(result);
	item.setOsName(osName);

	EntityItemStringType osRelease = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("uname -r", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	osRelease.setValue(result);
	item.setOsRelease(osRelease);

	EntityItemStringType osVersion = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("uname -v", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	osVersion.setValue(result);
	item.setOsVersion(osVersion);

	EntityItemStringType processorType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("uname -p", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	processorType.setValue(result);
	item.setProcessorType(processorType);

	return JOVALSystem.factories.sc.unix.createUnameItem(item);
    }
}
