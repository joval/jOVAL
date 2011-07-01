// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateAnySimpleType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.windows.WmiObject;
import oval.schemas.definitions.windows.WmiState;
import oval.schemas.definitions.windows.WmiTest;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.windows.WmiItem;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * Evaluates WmiTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IDefinitions definitions;
    private IWmiProvider wmi;

    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private oval.schemas.systemcharacteristics.windows.ObjectFactory windowsFactory;

    public WmiAdapter(IWmiProvider wmi) {
	this.wmi = wmi;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	windowsFactory = new oval.schemas.systemcharacteristics.windows.ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	definitions = ctx.getDefinitions();
	this.ctx = ctx;
    }

    public Class getTestClass() {
	return WmiTest.class;
    }

    public Class getObjectClass() {
	return WmiObject.class;
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	try {
	    wmi.connect();
	    Iterator <ObjectType>iter = definitions.iterateObjects(WmiObject.class);
	    while (iter.hasNext()) {
		WmiObject wObj = (WmiObject)iter.next();
		if (wObj.isSetSet()) {
		    // Set objects merely contain references to other objects that are scanned elsewhere.
		    continue;
		} else {
		    ctx.status(wObj.getId());
		    sc.setObject(wObj.getId(), wObj.getComment(), wObj.getVersion(), FlagEnumeration.COMPLETE, null);
		    BigInteger itemId = sc.storeItem(windowsFactory.createWmiItem(getItem(wObj)));
		    sc.relateItem(wObj.getId(), itemId);
		}
	    }
	} finally {
	    wmi.disconnect();
	}
    }

    public String getItemData(ObjectComponentType object, ISystemCharacteristics sc) throws OvalException {
	throw new OvalException("getItemData not supported for WmiObject");
    }

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	WmiTest test = definitions.getTest(testId, WmiTest.class);
	String objectId = test.getObject().getObjectRef();

	WmiState state = null;
	if (test.isSetState()) {
	    String stateId = test.getState().get(0).getStateRef();
	    state = definitions.getState(stateId, WmiState.class);
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.resultsFactory.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	boolean result = false;
	int trueCount=0, falseCount=0, errorCount=0;
	if (sc.getObject(objectId).getFlag() == FlagEnumeration.ERROR) {
	    errorCount++;
	}
	Iterator<ItemType> items = sc.getItemsByObjectId(objectId).iterator();
	switch (test.getCheckExistence()) {
	  case NONE_EXIST: {
            while(items.hasNext()) {
                ItemType it = items.next();
                if (it instanceof WmiItem) {
                    WmiItem item = (WmiItem)it;
                    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
                    testedItem.setItemId(item.getId());
                    switch(item.getStatus()) {
                      case EXISTS:
			if (hasResult(item)) {
                            trueCount++;
			} else {
                            falseCount++;
			}
                        testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
                        break;
                      case DOES_NOT_EXIST:
                        falseCount++;
                        testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
                        break;
                      case ERROR:
                        errorCount++;
                        testedItem.setResult(ResultEnumeration.ERROR);
                        break;
                      case NOT_COLLECTED:
                        testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
                        break;
                    }
                    testResult.getTestedItem().add(testedItem);
                } else {
                    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
                                                                   WmiItem.class.getName(), it.getClass().getName()));
                }
            }
            result = trueCount == 0;
	    break;
	  }

	  case AT_LEAST_ONE_EXISTS: {
            while(items.hasNext()) {
                ItemType it = items.next();
                if (it instanceof WmiItem) {
                    WmiItem item = (WmiItem)it;
                    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
                    testedItem.setItemId(item.getId());
                    switch(item.getStatus()) {
                      case EXISTS:
                        if (state == null) {
                            if (hasResult(item)) {
				trueCount++;
                        	testedItem.setResult(ResultEnumeration.TRUE);
			    } else {
				falseCount++;
                        	testedItem.setResult(ResultEnumeration.FALSE);
			    }
                        } else if (hasResult(item)) {
                            if(match(state, item)) {
                                trueCount++;
                                testedItem.setResult(ResultEnumeration.TRUE);
                            } else {
                                falseCount++;
                                testedItem.setResult(ResultEnumeration.FALSE);
                            }
                        } else {
                            falseCount++;
                            testedItem.setResult(ResultEnumeration.FALSE);
			}
                        break;
                      case DOES_NOT_EXIST:
                        falseCount++;
                        testedItem.setResult(ResultEnumeration.FALSE);
                        break;
                      case ERROR:
                        errorCount++;
                        testedItem.setResult(ResultEnumeration.ERROR);
                        break;
                      case NOT_COLLECTED:
                        testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
                        break;
                    }
                    testResult.getTestedItem().add(testedItem);
                } else {
                    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
                                                                   WmiItem.class.getName(), it.getClass().getName()));
                }
            }

            switch(test.getCheck()) {
              case ALL:
                result = falseCount == 0 && trueCount > 0;
                break;
              case NONE_SATISFY:
                result = trueCount == 0;
                break;
              case AT_LEAST_ONE:
                result = trueCount > 0;
                break;
              default:
                throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", test.getCheck()));
            }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE", test.getCheckExistence()));
	}

	if (errorCount > 0) {
	    testResult.setResult(ResultEnumeration.ERROR);
	} else if (result) {
	    testResult.setResult(ResultEnumeration.TRUE);
	} else {
	    testResult.setResult(ResultEnumeration.FALSE);
	}
    }

    // Private

    private boolean hasResult(WmiItem item) {
	List<EntityItemAnySimpleType> results = item.getResult();
	switch(results.size()) {
	  case 0:
	    return false;
	  case 1:
	    return results.get(0).getStatus() == StatusEnumeration.EXISTS;
	  default:
	    return true;
	}
    }

    private boolean match(WmiState state, WmiItem item) throws OvalException {
	Iterator<EntityItemAnySimpleType> results = item.getResult().iterator();
	EntityStateAnySimpleType stateEntity = state.getResult();
	switch (stateEntity.getOperation()) {
	  case PATTERN_MATCH: {
	    Pattern p = Pattern.compile((String)stateEntity.getValue());
	    while(results.hasNext()) {
		EntityItemAnySimpleType resultEntity = results.next();
		Matcher m = p.matcher((String)resultEntity.getValue());
		if (m.matches()) {
		    return true;
		}
	    }
	    break;
	  }
	  case EQUALS: {
	    while(results.hasNext()) {
		EntityItemAnySimpleType resultEntity = results.next();
		if (((String)resultEntity.getValue()).equals((String)stateEntity.getValue())) {
		    return true;
		}
	    }
	    break;
	  }
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", stateEntity.getOperation()));
	}
	return false;
    }

    private WmiItem getItem(WmiObject wObj) {
	String id = wObj.getId();
	WmiItem item = windowsFactory.createWmiItem();
	String ns = wObj.getNamespace().getValue().toString();
	EntityItemStringType namespaceType = coreFactory.createEntityItemStringType();
	namespaceType.setValue(ns);
	item.setNamespace(namespaceType);
	String wql = wObj.getWql().getValue().toString();
	EntityItemStringType wqlType = coreFactory.createEntityItemStringType();
	wqlType.setValue(wql);
	item.setWql(wqlType);
	try {
	    ISWbemObjectSet objSet = wmi.execQuery(ns, wql);
	    List<EntityItemAnySimpleType> result = item.getResult();
	    int size = objSet.getSize();
	    if (size == 0) {
		EntityItemAnySimpleType itemEntity = coreFactory.createEntityItemAnySimpleType();
		itemEntity.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		result.add(itemEntity);
	    } else {
		String field = getField(wql);
		for (ISWbemObject swbObj : objSet) {
		    for (ISWbemProperty prop : swbObj.getProperties()) {
			if (prop.getName().equalsIgnoreCase(field)) {
			    EntityItemAnySimpleType itemEntity = coreFactory.createEntityItemAnySimpleType();
			    itemEntity.setValue(prop.getValueAsString());
			    result.add(itemEntity);
			    break;
			}
		    }
		}
	    }
	} catch (WmiException e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    item.unsetResult();
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	} catch (Exception e) {
	    ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINWMI_GENERAL", id), e);
	}
	return item;
    }

    private String getField(String wql) {
	wql = wql.toUpperCase();
	StringTokenizer tok = new StringTokenizer(wql);
	while(tok.hasMoreTokens()) {
	    String token = tok.nextToken();
	    if (token.equals("SELECT")) {
		if (tok.hasMoreTokens()) {
		    return tok.nextToken();
		}
	    }
	}
	return null;
    }
}
