// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

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
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

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

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    items.add(getItem());
	} catch (Exception e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Internal

    private JAXBElement<UnameItem> getItem() throws Exception {
	UnameItem item = JOVALSystem.factories.sc.unix.createUnameItem();
	EntityItemStringType machineClass = JOVALSystem.factories.sc.core.createEntityItemStringType();
	machineClass.setValue(SafeCLI.exec("uname -m", session, IUnixSession.Timeout.S));
	item.setMachineClass(machineClass);

	EntityItemStringType nodeName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	nodeName.setValue(SafeCLI.exec("uname -n", session, IUnixSession.Timeout.S));
	item.setNodeName(nodeName);

	EntityItemStringType osName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	osName.setValue(SafeCLI.exec("uname -s", session, IUnixSession.Timeout.S));
	item.setOsName(osName);

	EntityItemStringType osRelease = JOVALSystem.factories.sc.core.createEntityItemStringType();
	osRelease.setValue(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
	item.setOsRelease(osRelease);

	EntityItemStringType osVersion = JOVALSystem.factories.sc.core.createEntityItemStringType();
	osVersion.setValue(SafeCLI.exec("uname -v", session, IUnixSession.Timeout.S));
	item.setOsVersion(osVersion);

	EntityItemStringType processorType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	processorType.setValue(SafeCLI.exec("uname -p", session, IUnixSession.Timeout.S));
	item.setProcessorType(processorType);

	return JOVALSystem.factories.sc.unix.createUnameItem(item);
    }
}
