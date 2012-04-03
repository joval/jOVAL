// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.solaris;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.solaris.IsainfoObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.solaris.IsainfoItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

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

    public Collection<Class> init(IBaseSession session) {
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
