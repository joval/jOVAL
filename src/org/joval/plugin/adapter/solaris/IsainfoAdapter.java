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
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityStateIntType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.IsainfoObject;
import oval.schemas.definitions.solaris.IsainfoState;
import oval.schemas.definitions.solaris.IsainfoTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.solaris.IsainfoItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates IsainfoTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IsainfoAdapter implements IAdapter {
    private IAdapterContext ctx;
    private ISession session;

    public IsainfoAdapter(ISession session) {
	this.session = session;
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return IsainfoObject.class;
    }

    public Class getStateClass() {
	return IsainfoState.class;
    }

    public Class getItemClass() {
	return IsainfoItem.class;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    items.add(getItem());
	} catch (Exception e) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    ctx.addObjectMessage(obj.getId(), msg);
	    ctx.log(Level.WARNING, e.getMessage(), e);
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	IsainfoState state = (IsainfoState)st;
	IsainfoItem item = (IsainfoItem)it;

	if (state.isSetApplicationIsa()) {
	    return ctx.test(state.getApplicationIsa(), item.getApplicationIsa());
	} else if (state.isSetKernelIsa()) {
	    return ctx.test(state.getKernelIsa(), item.getKernelIsa());
	} else if (state.isSetBits()) {
	    return ctx.test(state.getBits(), item.getBits());
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
	}
    }

    // Internal

    private JAXBElement<IsainfoItem> getItem() throws Exception {
	IsainfoItem item = JOVALSystem.factories.sc.solaris.createIsainfoItem();
	EntityItemStringType kernelIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	IProcess p = session.createProcess("isainfo -k");
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();
	kernelIsa.setValue(result);
	item.setKernelIsa(kernelIsa);

	EntityItemStringType applicationIsa = JOVALSystem.factories.sc.core.createEntityItemStringType();
	p = session.createProcess("isainfo -n");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	applicationIsa.setValue(result);
	item.setApplicationIsa(applicationIsa);

	EntityItemIntType bits = JOVALSystem.factories.sc.core.createEntityItemIntType();
	p = session.createProcess("isainfo -b");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	bits.setValue(result);
	bits.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setBits(bits);

	return JOVALSystem.factories.sc.solaris.createIsainfoItem(item);
    }
}
