// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.solaris.IsainfoObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.solaris.IsainfoItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Evaluates IsainfoTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IsainfoAdapter implements IAdapter {
    private IUnixSession session;

    public IsainfoAdapter(IUnixSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {IsainfoObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) {
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

    private JAXBElement<IsainfoItem> getItem() throws Exception {
	IsainfoItem item = JOVALSystem.factories.sc.solaris.createIsainfoItem();
	EntityItemStringType kernelIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	kernelIsa.setValue(SafeCLI.exec("isainfo -k", session, IUnixSession.Timeout.S));
	item.setKernelIsa(kernelIsa);

	EntityItemStringType applicationIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	applicationIsa.setValue(SafeCLI.exec("isainfo -n", session, IUnixSession.Timeout.S));
	item.setApplicationIsa(applicationIsa);

	EntityItemIntType bits = JOVALSystem.factories.sc.core.createEntityItemIntType();
	bits.setValue(SafeCLI.exec("isainfo -b", session, IUnixSession.Timeout.S));
	bits.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setBits(bits);

	return JOVALSystem.factories.sc.solaris.createIsainfoItem(item);
    }
}
