// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Vector;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.IsainfoObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.solaris.IsainfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates IsainfoTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IsainfoAdapter implements IAdapter {
    private IUnixSession session;
    private IsainfoItem item = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(IsainfoObject.class);
	}
	return classes;
    }

    public Collection<IsainfoItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<IsainfoItem> items = new Vector<IsainfoItem>();
	try {
	    if (item == null) {
		item = Factories.sc.solaris.createIsainfoItem();
		EntityItemStringType kernelIsa = Factories.sc.core.createEntityItemStringType();
		kernelIsa.setValue(SafeCLI.exec("isainfo -k", session, IUnixSession.Timeout.S));
		item.setKernelIsa(kernelIsa);

		EntityItemStringType applicationIsa = Factories.sc.core.createEntityItemStringType();
		applicationIsa.setValue(SafeCLI.exec("isainfo -n", session, IUnixSession.Timeout.S));
		item.setApplicationIsa(applicationIsa);

		EntityItemIntType bits = Factories.sc.core.createEntityItemIntType();
		bits.setValue(SafeCLI.exec("isainfo -b", session, IUnixSession.Timeout.S));
		bits.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setBits(bits);
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
