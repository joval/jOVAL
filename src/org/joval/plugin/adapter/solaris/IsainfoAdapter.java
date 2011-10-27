// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

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

    public Class getObjectClass() {
	return IsainfoObject.class;
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
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Internal

    private JAXBElement<IsainfoItem> getItem() throws Exception {
	IsainfoItem item = JOVALSystem.factories.sc.solaris.createIsainfoItem();
	EntityItemStringType kernelIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	IProcess p = session.createProcess("isainfo -k", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();
	p.waitFor(0);
	kernelIsa.setValue(result);
	item.setKernelIsa(kernelIsa);

	EntityItemStringType applicationIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("isainfo -n", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	applicationIsa.setValue(result);
	item.setApplicationIsa(applicationIsa);

	EntityItemIntType bits = JOVALSystem.factories.sc.core.createEntityItemIntType();
	p = session.createProcess("isainfo -b", IUnixSession.TIMEOUT_S, IUnixSession.DEBUG);
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	p.waitFor(0);
	bits.setValue(result);
	bits.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setBits(bits);

	return JOVALSystem.factories.sc.solaris.createIsainfoItem(item);
    }
}
