// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.unix.UnameObject;
import oval.schemas.definitions.unix.UnameState;
import oval.schemas.definitions.unix.UnameTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.unix.UnameItem;
import oval.schemas.systemcharacteristics.unix.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UnameTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnameAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IDefinitions definitions;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory unixFactory;

    public UnameAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	unixFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
	definitions = ctx.getDefinitions();
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	Iterator<ObjectType> iter = definitions.iterateObjects(UnameObject.class);
	for (int i=0; iter.hasNext(); i++) {
	    if (i > 0) {
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNAME_OVERFLOW"));
	    }
	    UnameObject uObj = (UnameObject)iter.next();
	    ctx.status(uObj.getId());
	    try {
		JAXBElement<UnameItem> item = getItem();
		sc.setObject(uObj.getId(), uObj.getComment(), uObj.getVersion(), FlagEnumeration.COMPLETE, null);
		BigInteger itemId = sc.storeItem(item);
		sc.relateItem(uObj.getId(), itemId);
	    } catch (Exception e) {
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		sc.setObject(uObj.getId(), uObj.getComment(), uObj.getVersion(), FlagEnumeration.ERROR, msg);
	    }
	}
    }

    public Class getObjectClass() {
	return UnameObject.class;
    }

    public Class getTestClass() {
	return UnameTest.class;
    }

    public String getItemData(ObjectComponentType object, ISystemCharacteristics sc) throws OvalException {
	return null; // What foolish variable would point to a UnameObject?
    }

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	UnameTest testDefinition = ctx.getDefinitions().getTest(testId, UnameTest.class); 
	String objectId = testDefinition.getObject().getObjectRef();
	UnameObject uObj = definitions.getObject(objectId, UnameObject.class);

	//
	// Decode the state object
	//
	UnameState uState = null;
	String stateId = testDefinition.getState().get(0).getStateRef();
	if (stateId != null) {
	    uState = definitions.getState(stateId, UnameState.class);
	}

	ItemType it = sc.getItemsByObjectId(objectId).get(0);
	if (!(it instanceof UnameItem)) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   UnameItem.class.getName(), it.getClass().getName()));
	}
	UnameItem item = (UnameItem)it;

	if (uState.isSetMachineClass()) {
	    testResult.setResult(compareTypes(uState.getMachineClass(), item.getMachineClass()));
	} else if (uState.isSetNodeName()) {
	    testResult.setResult(compareTypes(uState.getNodeName(), item.getNodeName()));
	} else if (uState.isSetOsName()) {
	    testResult.setResult(compareTypes(uState.getOsName(), item.getOsName()));
	} else if (uState.isSetOsRelease()) {
	    testResult.setResult(compareTypes(uState.getOsRelease(), item.getOsRelease()));
	} else if (uState.isSetOsVersion()) {
	    testResult.setResult(compareTypes(uState.getOsVersion(), item.getOsVersion()));
	} else if (uState.isSetProcessorType()) {
	    testResult.setResult(compareTypes(uState.getProcessorType(), item.getProcessorType()));
	} else {
	    testResult.setResult(ResultEnumeration.ERROR);
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage("ERROR_UNAME_STATE_EMPTY", stateId));
	    testResult.getMessage().add(msg);
	}
    }

    // Internal

    private ResultEnumeration compareTypes(EntityStateStringType state, EntityItemStringType item) throws OvalException {
	switch (state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    if (((String)item.getValue()).equalsIgnoreCase((String)state.getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case EQUALS:
	    if (((String)item.getValue()).equals((String)state.getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case PATTERN_MATCH:
	    if (Pattern.matches((String)state.getValue(), (String)item.getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

    private JAXBElement<UnameItem> getItem() throws Exception {
	UnameItem item = unixFactory.createUnameItem();
	EntityItemStringType machineClass = coreFactory.createEntityItemStringType();
	IProcess p = session.createProcess("uname -m");
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();
	machineClass.setValue(result);
	item.setMachineClass(machineClass);

	EntityItemStringType nodeName = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -n");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	nodeName.setValue(result);
	item.setNodeName(nodeName);

	EntityItemStringType osName = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -s");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osName.setValue(result);
	item.setOsName(osName);

	EntityItemStringType osRelease = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -r");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osRelease.setValue(result);
	item.setOsRelease(osRelease);

	EntityItemStringType osVersion = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -v");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osVersion.setValue(result);
	item.setOsVersion(osVersion);

	EntityItemStringType processorType = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -p");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	processorType.setValue(result);
	item.setProcessorType(processorType);

	return unixFactory.createUnameItem(item);
    }
}
