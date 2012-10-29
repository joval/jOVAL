// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;

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
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Evaluates UnameTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnameAdapter implements IAdapter {
    private IUnixSession session;
    private UnameItem item;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(UnameObject.class);
	}
	return classes;
    }

    public Collection<UnameItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<UnameItem> items = new Vector<UnameItem>();
	try {
	    if (item == null) {
		item = Factories.sc.unix.createUnameItem();
		EntityItemStringType machineClass = Factories.sc.core.createEntityItemStringType();
		machineClass.setValue(SafeCLI.exec("uname -m", session, IUnixSession.Timeout.S));
		item.setMachineClass(machineClass);

		EntityItemStringType nodeName = Factories.sc.core.createEntityItemStringType();
		nodeName.setValue(SafeCLI.exec("uname -n", session, IUnixSession.Timeout.S));
		item.setNodeName(nodeName);

		EntityItemStringType osName = Factories.sc.core.createEntityItemStringType();
		osName.setValue(SafeCLI.exec("uname -s", session, IUnixSession.Timeout.S));
		item.setOsName(osName);

		EntityItemStringType osRelease = Factories.sc.core.createEntityItemStringType();
		osRelease.setValue(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
		item.setOsRelease(osRelease);

		EntityItemStringType osVersion = Factories.sc.core.createEntityItemStringType();
		osVersion.setValue(SafeCLI.exec("uname -v", session, IUnixSession.Timeout.S));
		item.setOsVersion(osVersion);

		EntityItemStringType processorType = Factories.sc.core.createEntityItemStringType();
		processorType.setValue(SafeCLI.exec("uname -p", session, IUnixSession.Timeout.S));
		item.setProcessorType(processorType);
	    }
	    items.add(item);
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }
}
